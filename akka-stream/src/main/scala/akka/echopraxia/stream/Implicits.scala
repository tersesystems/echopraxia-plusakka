package akka.echopraxia.stream

import akka.stream.scaladsl._
import com.tersesystems.echopraxia.api.Field

import scala.annotation.unchecked.uncheckedVariance

trait Implicits {

  // You can't use a LoggingAdapter here at all, because LoggingAdapter throws away the original
  // arguments and only logs a string template -- so logstash structuredArguments won't translate
  // well and you can't get JSON into the underlying logger.
  //
  // You also have to import the implicits to add methods to the flow, so `.debug` becomes an option.

  implicit class SourceLogging[Out, Mat](s: Source[Out, Mat]) {

    def log2[FB <: AkkaStreamFieldBuilder](implicit log: EchopraxiaLoggingAdapter[FB]): LoggingStage[FB] = {
      new LoggingStage[FB](log)
    }

    class LoggingStage[FB <: AkkaStreamFieldBuilder](log: EchopraxiaLoggingAdapter[FB]) {
      def debug(name: String, extract: (FB, Out) => Field): Source[Out, Mat] = {
        s.via(logStage(name, extract, log))
      }
    }
  }

  implicit class SourceWithContextLogging[Out, Ctx, Mat](s: SourceWithContext[Out, Ctx, Mat]) {
    type ReprMat[+O, +C, +M] = SourceWithContext[O, C, M @uncheckedVariance]
    type Repr[+O, +C] = ReprMat[O, C, Mat @uncheckedVariance]

    def log2[FB <: AkkaStreamFieldBuilder](implicit log: EchopraxiaLoggingAdapter[FB]): LoggingStage[FB] = {
      new LoggingStage(log)
    }

    class LoggingStage[FB <: AkkaStreamFieldBuilder](log: EchopraxiaLoggingAdapter[FB]) {
      def debug(name: String, extract: (FB, Out) => Field): Repr[Out, Ctx] = {
        val extractWithContext: (FB, (Out, Ctx)) => Field = {
          case (fb, (e, _)) => extract(fb, e)
        }
        s.via(Flow[(Out, Ctx)].log2(log).debug(name, extractWithContext))
      }
    }
  }

  implicit class FlowLogging[In, Out, Mat](s: Flow[In, Out, Mat]) {

    def log2[FB <: AkkaStreamFieldBuilder](implicit log: EchopraxiaLoggingAdapter[FB]): LoggingStage[FB] = {
      new LoggingStage[FB](log)
    }

    class LoggingStage[FB <: AkkaStreamFieldBuilder](log: EchopraxiaLoggingAdapter[FB]) {
      def debug(name: String, extract: (FB, Out) => Field): Flow[In, Out, Mat] = {
        s.via(logStage(name, extract, log))
      }
    }
  }

  implicit class FlowWithContextLogging[In, Out, Ctx, Mat](flow: FlowWithContext[In, Ctx, Out, Ctx, Mat]) {
    type ReprMat[+O, +C, +M] =
      FlowWithContext[In@uncheckedVariance, Ctx@uncheckedVariance, O, C, M@uncheckedVariance]
    type Repr[+O, +C] = ReprMat[O, C, Mat @uncheckedVariance]

    def log2[FB <: AkkaStreamFieldBuilder](implicit log: EchopraxiaLoggingAdapter[FB]): LoggingStage[FB] = {
      new LoggingStage[FB](log)
    }

    class LoggingStage[FB <: AkkaStreamFieldBuilder](log: EchopraxiaLoggingAdapter[FB]) {
      def debug(name: String, extract: (FB, Out) => Field): Repr[Out, Ctx] = {
        val extractWithContext: (FB, (Out, Ctx)) => Field = {
          case (fb, (e, _)) => extract(fb, e)
        }
        flow.via(logStage(name, extractWithContext, log))
      }
    }
  }

  private def logStage[Out, FB <: AkkaStreamFieldBuilder](name: String,
                                                          extract: (FB, Out) => Field,
                                                          log: EchopraxiaLoggingAdapter[FB]): DebugLog[FB, Out] = {
    new DebugLog[FB, Out](name, extract, DebugLog.NameKey, DebugLog.OperationKey, DebugLog.CauseKey, log)
  }
}


object Implicits extends Implicits