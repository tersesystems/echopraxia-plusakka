package akka.echopraxia.stream

import akka.stream.scaladsl._
import com.tersesystems.echopraxia.api.{CoreLogger, Field, FieldBuilderResult, Level, Utilities}
import com.tersesystems.echopraxia.plusscala.api.Condition

import scala.annotation.unchecked.uncheckedVariance
import scala.compat.java8.FunctionConverters.enrichAsJavaFunction

trait Implicits {

  implicit class SourceLogging[Out, Mat](s: Source[Out, Mat]) {

    def log2[FB <: AkkaStreamFieldBuilder](implicit log: EchopraxiaLoggingAdapter[FB]): LoggingStage[FB] = {
      new LoggingStage[FB](log.core, log.fieldBuilder)
    }

    class LoggingStage[FB <: AkkaStreamFieldBuilder](core: CoreLogger, fieldBuilder: FB) {

      def withCondition(condition: Condition): LoggingStage[FB] = {
        new LoggingStage[FB](core.withCondition(condition.asJava), fieldBuilder)
      }

      def withFields(fbf: FB => FieldBuilderResult): LoggingStage[FB] = {
        new LoggingStage[FB](core.withFields(fbf.asJava, fieldBuilder), fieldBuilder)
      }

      def withThreadContext: LoggingStage[FB] = {
        new LoggingStage[FB](core.withThreadContext(Utilities.threadContext()), fieldBuilder)
      }

      def trace(name: String, extract: (FB, Out) => Field): Source[Out, Mat] = {
        s.via(logStage(Level.TRACE, name, extract, core, fieldBuilder))
      }

      def debug(name: String, extract: (FB, Out) => Field): Source[Out, Mat] = {
        s.via(logStage(Level.DEBUG, name, extract, core, fieldBuilder))
      }

      def info(name: String, extract: (FB, Out) => Field): Source[Out, Mat] = {
        s.via(logStage(Level.INFO, name, extract, core, fieldBuilder))
      }

      def warn(name: String, extract: (FB, Out) => Field): Source[Out, Mat] = {
        s.via(logStage(Level.WARN, name, extract, core, fieldBuilder))
      }

      def error(name: String, extract: (FB, Out) => Field): Source[Out, Mat] = {
        s.via(logStage(Level.ERROR, name, extract, core, fieldBuilder))
      }
    }
  }

  implicit class SourceWithContextLogging[Out, Ctx, Mat](s: SourceWithContext[Out, Ctx, Mat]) {
    type ReprMat[+O, +C, +M] = SourceWithContext[O, C, M@uncheckedVariance]
    type Repr[+O, +C] = ReprMat[O, C, Mat@uncheckedVariance]

    def log2[FB <: AkkaStreamFieldBuilder](implicit log: EchopraxiaLoggingAdapter[FB]): LoggingStage[FB] = {
      new LoggingStage(log.core, log.fieldBuilder)
    }

    class LoggingStage[FB <: AkkaStreamFieldBuilder](core: CoreLogger, fieldBuilder: FB) {

      private val self = new EchopraxiaLoggingAdapter[FB] {
        override def core: CoreLogger = LoggingStage.this.core
        override def fieldBuilder: FB = LoggingStage.this.fieldBuilder
      }

      def withCondition(condition: Condition): LoggingStage[FB] = {
        new LoggingStage[FB](core.withCondition(condition.asJava), fieldBuilder)
      }

      def withFields(fbf: FB => FieldBuilderResult): LoggingStage[FB] = {
        new LoggingStage[FB](core.withFields(fbf.asJava, fieldBuilder), fieldBuilder)
      }

      def withThreadContext: LoggingStage[FB] = {
        new LoggingStage[FB](core.withThreadContext(Utilities.threadContext()), fieldBuilder)
      }

      def trace(name: String, extract: (FB, Out) => Field): Repr[Out, Ctx] = {
        val extractWithContext: (FB, (Out, Ctx)) => Field = {
          case (fb, (e, _)) => extract(fb, e)
        }
        s.via(Flow[(Out, Ctx)].log2(self).trace(name, extractWithContext))
      }

      def debug(name: String, extract: (FB, Out) => Field): Repr[Out, Ctx] = {
        val extractWithContext: (FB, (Out, Ctx)) => Field = {
          case (fb, (e, _)) => extract(fb, e)
        }
        s.via(Flow[(Out, Ctx)].log2(self).debug(name, extractWithContext))
      }

      def info(name: String, extract: (FB, Out) => Field): Repr[Out, Ctx] = {
        val extractWithContext: (FB, (Out, Ctx)) => Field = {
          case (fb, (e, _)) => extract(fb, e)
        }
        s.via(Flow[(Out, Ctx)].log2(self).info(name, extractWithContext))
      }

      def warn(name: String, extract: (FB, Out) => Field): Repr[Out, Ctx] = {
        val extractWithContext: (FB, (Out, Ctx)) => Field = {
          case (fb, (e, _)) => extract(fb, e)
        }
        s.via(Flow[(Out, Ctx)].log2(self).warn(name, extractWithContext))
      }

      def error(name: String, extract: (FB, Out) => Field): Repr[Out, Ctx] = {
        val extractWithContext: (FB, (Out, Ctx)) => Field = {
          case (fb, (e, _)) => extract(fb, e)
        }
        s.via(Flow[(Out, Ctx)].log2(self).error(name, extractWithContext))
      }
    }
  }

  implicit class FlowLogging[In, Out, Mat](s: Flow[In, Out, Mat]) {

    def log2[FB <: AkkaStreamFieldBuilder](implicit log: EchopraxiaLoggingAdapter[FB]): LoggingStage[FB] = {
      new LoggingStage[FB](log.core, log.fieldBuilder)
    }

    class LoggingStage[FB <: AkkaStreamFieldBuilder](core: CoreLogger, fieldBuilder: FB) {

      def withCondition(condition: Condition): LoggingStage[FB] = {
        new LoggingStage[FB](core.withCondition(condition.asJava), fieldBuilder)
      }

      def withFields(fbf: FB => FieldBuilderResult): LoggingStage[FB] = {
        new LoggingStage[FB](core.withFields(fbf.asJava, fieldBuilder), fieldBuilder)
      }

      def withThreadContext: LoggingStage[FB] = {
        new LoggingStage[FB](core.withThreadContext(Utilities.threadContext()), fieldBuilder)
      }

      def trace(name: String, extract: (FB, Out) => Field): Flow[In, Out, Mat] = {
        s.via(logStage(Level.TRACE, name, extract, core, fieldBuilder))
      }

      def debug(name: String, extract: (FB, Out) => Field): Flow[In, Out, Mat] = {
        s.via(logStage(Level.DEBUG, name, extract, core, fieldBuilder))
      }

      def info(name: String, extract: (FB, Out) => Field): Flow[In, Out, Mat] = {
        s.via(logStage(Level.INFO, name, extract, core, fieldBuilder))
      }

      def warn(name: String, extract: (FB, Out) => Field): Flow[In, Out, Mat] = {
        s.via(logStage(Level.WARN, name, extract, core, fieldBuilder))
      }

      def error(name: String, extract: (FB, Out) => Field): Flow[In, Out, Mat] = {
        s.via(logStage(Level.ERROR, name, extract, core, fieldBuilder))
      }
    }
  }

  implicit class FlowWithContextLogging[In, Out, Ctx, Mat](flow: FlowWithContext[In, Ctx, Out, Ctx, Mat]) {
    type ReprMat[+O, +C, +M] =
      FlowWithContext[In@uncheckedVariance, Ctx@uncheckedVariance, O, C, M@uncheckedVariance]
    type Repr[+O, +C] = ReprMat[O, C, Mat@uncheckedVariance]

    def log2[FB <: AkkaStreamFieldBuilder](implicit log: EchopraxiaLoggingAdapter[FB]): LoggingStage[FB] = {
      new LoggingStage[FB](log.core, log.fieldBuilder)
    }

    class LoggingStage[FB <: AkkaStreamFieldBuilder](core: CoreLogger, fieldBuilder: FB) {

      def withCondition(condition: Condition): LoggingStage[FB] = {
        new LoggingStage[FB](core.withCondition(condition.asJava), fieldBuilder)
      }

      def withFields(fbf: FB => FieldBuilderResult): LoggingStage[FB] = {
        new LoggingStage[FB](core.withFields(fbf.asJava, fieldBuilder), fieldBuilder)
      }

      def withThreadContext: LoggingStage[FB] = {
        new LoggingStage[FB](core.withThreadContext(Utilities.threadContext()), fieldBuilder)
      }

      def trace(name: String, extract: (FB, Out) => Field): Repr[Out, Ctx] = {
        val extractWithContext: (FB, (Out, Ctx)) => Field = {
          case (fb, (e, _)) => extract(fb, e)
        }
        flow.via(logStage(Level.TRACE, name, extractWithContext, core, fieldBuilder))
      }

      def debug(name: String, extract: (FB, Out) => Field): Repr[Out, Ctx] = {
        val extractWithContext: (FB, (Out, Ctx)) => Field = {
          case (fb, (e, _)) => extract(fb, e)
        }
        flow.via(logStage(Level.DEBUG, name, extractWithContext, core, fieldBuilder))
      }

      def info(name: String, extract: (FB, Out) => Field): Repr[Out, Ctx] = {
        val extractWithContext: (FB, (Out, Ctx)) => Field = {
          case (fb, (e, _)) => extract(fb, e)
        }
        flow.via(logStage(Level.INFO, name, extractWithContext, core, fieldBuilder))
      }

      def warn(name: String, extract: (FB, Out) => Field): Repr[Out, Ctx] = {
        val extractWithContext: (FB, (Out, Ctx)) => Field = {
          case (fb, (e, _)) => extract(fb, e)
        }
        flow.via(logStage(Level.WARN, name, extractWithContext, core, fieldBuilder))
      }

      def error(name: String, extract: (FB, Out) => Field): Repr[Out, Ctx] = {
        val extractWithContext: (FB, (Out, Ctx)) => Field = {
          case (fb, (e, _)) => extract(fb, e)
        }
        flow.via(logStage(Level.ERROR, name, extractWithContext, core, fieldBuilder))
      }
    }
  }

  private def logStage[Out, FB <: AkkaStreamFieldBuilder](level: Level,
                                                          name: String,
                                                          extract: (FB, Out) => Field,
                                                          core: CoreLogger,
                                                          fieldBuilder: FB): Log2[FB, Out] = {
    new Log2[FB, Out](level, name, extract, Log2.NameKey, Log2.OperationKey, Log2.CauseKey, core, fieldBuilder)
  }

}


object Implicits extends Implicits