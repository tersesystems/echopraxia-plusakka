package akka.echopraxia.stream

import akka.echopraxia.actor.EchopraxiaLoggingAdapter
import akka.stream.scaladsl._
import com.tersesystems.echopraxia.api._
import com.tersesystems.echopraxia.plusscala.api.Condition

import scala.annotation.unchecked.uncheckedVariance
import scala.compat.java8.FunctionConverters.enrichAsJavaFunction

trait Implicits {

  implicit class SourceLogging[Out, Mat](s: Source[Out, Mat]) {

    def elog[FB <: AkkaStreamFieldBuilder](implicit log: EchopraxiaLoggingAdapter[FB]): SourceLoggingStage[FB, Out, Mat] = {
      new SourceLoggingStage(s, log.core, log.fieldBuilder)
    }
  }

  implicit class SourceWithContextLogging[Out, Ctx, Mat](s: SourceWithContext[Out, Ctx, Mat]) {
    def elog[FB <: AkkaStreamFieldBuilder](implicit log: EchopraxiaLoggingAdapter[FB]): SourceWithContextLoggingStage[FB, Out, Ctx, Mat] = {
      new SourceWithContextLoggingStage(s, log.core, log.fieldBuilder)
    }
  }

  implicit class FlowLogging[In, Out, Mat](f: Flow[In, Out, Mat]) {

    def elog[FB <: AkkaStreamFieldBuilder](implicit log: EchopraxiaLoggingAdapter[FB]): FlowLoggingStage[FB, In, Out, Mat] = {
      new FlowLoggingStage(f, log.core, log.fieldBuilder)
    }
  }

  implicit class FlowWithContextLogging[In, Out, Ctx, Mat](flow: FlowWithContext[In, Ctx, Out, Ctx, Mat]) {
    def elog[FB <: AkkaStreamFieldBuilder](implicit log: EchopraxiaLoggingAdapter[FB]): FlowWithContextLoggingStage[FB, In, Out, Ctx, Mat] = {
      new FlowWithContextLoggingStage(flow, log.core, log.fieldBuilder)
    }
  }

}


object Implicits extends Implicits


class SourceLoggingStage[FB <: AkkaStreamFieldBuilder, Out, Mat](s: Source[Out, Mat], core: CoreLogger, fieldBuilder: FB) {

  def withCondition(condition: Condition): SourceLoggingStage[FB, Out, Mat] = {
    new SourceLoggingStage[FB, Out, Mat](s, core.withCondition(condition.asJava), fieldBuilder)
  }

  def withFields(fbf: FB => FieldBuilderResult): SourceLoggingStage[FB, Out, Mat] = {
    new SourceLoggingStage[FB, Out, Mat](s, core.withFields(fbf.asJava, fieldBuilder), fieldBuilder)
  }

  def withThreadContext: SourceLoggingStage[FB, Out, Mat] = {
    new SourceLoggingStage[FB, Out, Mat](s, core.withThreadContext(Utilities.threadContext()), fieldBuilder)
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

  private def logStage(level: Level,
                       name: String,
                       extract: (FB, Out) => Field,
                       core: CoreLogger,
                       fieldBuilder: FB): EchopraxiaLog[FB, Out] = {
    new EchopraxiaLog[FB, Out](level, name, extract, EchopraxiaLog.NameKey, EchopraxiaLog.OperationKey, EchopraxiaLog.CauseKey, core, fieldBuilder)
  }
}

class SourceWithContextLoggingStage[FB <: AkkaStreamFieldBuilder, Out, Ctx, Mat](s: SourceWithContext[Out, Ctx, Mat], core: CoreLogger, fieldBuilder: FB) {
  type ReprMat[+O, +C, +M] = SourceWithContext[O, C, M@uncheckedVariance]
  type Repr[+O, +C] = ReprMat[O, C, Mat@uncheckedVariance]

  private val self = new EchopraxiaLoggingAdapter[FB] {
    override def core: CoreLogger = SourceWithContextLoggingStage.this.core

    override def fieldBuilder: FB = SourceWithContextLoggingStage.this.fieldBuilder
  }

  def withCondition(condition: Condition): SourceWithContextLoggingStage[FB, Out, Ctx, Mat] = {
    new SourceWithContextLoggingStage(s, core.withCondition(condition.asJava), fieldBuilder)
  }

  def withFields(fbf: FB => FieldBuilderResult): SourceWithContextLoggingStage[FB, Out, Ctx, Mat] = {
    new SourceWithContextLoggingStage(s, core.withFields(fbf.asJava, fieldBuilder), fieldBuilder)
  }

  def withThreadContext: SourceWithContextLoggingStage[FB, Out, Ctx, Mat] = {
    new SourceWithContextLoggingStage(s, core.withThreadContext(Utilities.threadContext()), fieldBuilder)
  }

  def trace(name: String, extract: (FB, Out) => Field): Repr[Out, Ctx] = {
    val extractWithContext: (FB, (Out, Ctx)) => Field = {
      case (fb, (e, _)) => extract(fb, e)
    }
    import Implicits._
    s.via(Flow[(Out, Ctx)].elog(self).trace(name, extractWithContext))
  }

  def debug(name: String, extract: (FB, Out) => Field): Repr[Out, Ctx] = {
    val extractWithContext: (FB, (Out, Ctx)) => Field = {
      case (fb, (e, _)) => extract(fb, e)
    }

    import Implicits._
    s.via(Flow[(Out, Ctx)].elog(self).debug(name, extractWithContext))
  }

  def info(name: String, extract: (FB, Out) => Field): Repr[Out, Ctx] = {
    val extractWithContext: (FB, (Out, Ctx)) => Field = {
      case (fb, (e, _)) => extract(fb, e)
    }

    import Implicits._
    s.via(Flow[(Out, Ctx)].elog(self).info(name, extractWithContext))
  }

  def warn(name: String, extract: (FB, Out) => Field): Repr[Out, Ctx] = {
    val extractWithContext: (FB, (Out, Ctx)) => Field = {
      case (fb, (e, _)) => extract(fb, e)
    }

    import Implicits._
    s.via(Flow[(Out, Ctx)].elog(self).warn(name, extractWithContext))
  }

  def error(name: String, extract: (FB, Out) => Field): Repr[Out, Ctx] = {
    val extractWithContext: (FB, (Out, Ctx)) => Field = {
      case (fb, (e, _)) => extract(fb, e)
    }

    import Implicits._
    s.via(Flow[(Out, Ctx)].elog(self).error(name, extractWithContext))
  }
}

class FlowLoggingStage[FB <: AkkaStreamFieldBuilder, In, Out, Mat](f: Flow[In, Out, Mat], core: CoreLogger, fieldBuilder: FB) {

  def withCondition(condition: Condition): FlowLoggingStage[FB, In, Out, Mat] = {
    new FlowLoggingStage(f, core.withCondition(condition.asJava), fieldBuilder)
  }

  def withFields(fbf: FB => FieldBuilderResult): FlowLoggingStage[FB, In, Out, Mat] = {
    new FlowLoggingStage(f, core.withFields(fbf.asJava, fieldBuilder), fieldBuilder)
  }

  def withThreadContext: FlowLoggingStage[FB, In, Out, Mat] = {
    new FlowLoggingStage(f, core.withThreadContext(Utilities.threadContext()), fieldBuilder)
  }

  def trace(name: String, extract: (FB, Out) => Field): Flow[In, Out, Mat] = {
    f.via(logStage(Level.TRACE, name, extract, core, fieldBuilder))
  }

  def debug(name: String, extract: (FB, Out) => Field): Flow[In, Out, Mat] = {
    f.via(logStage(Level.DEBUG, name, extract, core, fieldBuilder))
  }

  def info(name: String, extract: (FB, Out) => Field): Flow[In, Out, Mat] = {
    f.via(logStage(Level.INFO, name, extract, core, fieldBuilder))
  }

  def warn(name: String, extract: (FB, Out) => Field): Flow[In, Out, Mat] = {
    f.via(logStage(Level.WARN, name, extract, core, fieldBuilder))
  }

  def error(name: String, extract: (FB, Out) => Field): Flow[In, Out, Mat] = {
    f.via(logStage(Level.ERROR, name, extract, core, fieldBuilder))
  }

  private def logStage(level: Level,
                       name: String,
                       extract: (FB, Out) => Field,
                       core: CoreLogger,
                       fieldBuilder: FB): EchopraxiaLog[FB, Out] = {
    new EchopraxiaLog[FB, Out](level, name, extract, EchopraxiaLog.NameKey, EchopraxiaLog.OperationKey, EchopraxiaLog.CauseKey, core, fieldBuilder)
  }

}

class FlowWithContextLoggingStage[FB <: AkkaStreamFieldBuilder, In, Out, Ctx, Mat](flow: FlowWithContext[In, Ctx, Out, Ctx, Mat], core: CoreLogger, fieldBuilder: FB) {
  type ReprMat[+O, +C, +M] =
    FlowWithContext[In@uncheckedVariance, Ctx@uncheckedVariance, O, C, M@uncheckedVariance]
  type Repr[+O, +C] = ReprMat[O, C, Mat@uncheckedVariance]

  def withCondition(condition: Condition): FlowWithContextLoggingStage[FB, In, Out, Ctx, Mat] = {
    new FlowWithContextLoggingStage(flow, core.withCondition(condition.asJava), fieldBuilder)
  }

  def withFields(fbf: FB => FieldBuilderResult): FlowWithContextLoggingStage[FB, In, Out, Ctx, Mat] = {
    new FlowWithContextLoggingStage(flow, core.withFields(fbf.asJava, fieldBuilder), fieldBuilder)
  }

  def withThreadContext: FlowWithContextLoggingStage[FB, In, Out, Ctx, Mat] = {
    new FlowWithContextLoggingStage(flow, core.withThreadContext(Utilities.threadContext()), fieldBuilder)
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

  def logStage(level: Level, name: String, extract: (FB, (Out, Ctx)) => Field, core: CoreLogger, fieldBuilder: FB): EchopraxiaLog[FB, (Out, Ctx)] = {
    new EchopraxiaLog(level, name, extract, EchopraxiaLog.NameKey, EchopraxiaLog.OperationKey, EchopraxiaLog.CauseKey, core, fieldBuilder)
  }
}