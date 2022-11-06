package akka.echopraxia.actor

import akka.actor.Actor.Receive
import akka.actor.{AbstractActor, ActorContext}
import akka.event.Logging
import akka.event.Logging.LogLevel
import com.tersesystems.echopraxia.api.{FieldBuilderResult, Level}

import java.util.Objects
import scala.compat.java8.FunctionConverters.enrichAsJavaFunction
import scala.language.existentials
import scala.runtime.BoxedUnit

object LoggingReceive {

  def apply[FB <: AkkaFieldBuilder](r: Receive)(implicit context: ActorContext, adapter: EchopraxiaLoggingAdapter[FB]): Receive = withLabel(null)(r)

  def apply[FB <: AkkaFieldBuilder](logLevel: LogLevel)(r: Receive)(implicit context: ActorContext, adapter: EchopraxiaLoggingAdapter[FB]): Receive = withLabel(null, logLevel)(r)

  def create[FB <: AkkaFieldBuilder](r: AbstractActor.Receive, context: AbstractActor.ActorContext, adapter: EchopraxiaLoggingAdapter[FB]): AbstractActor.Receive =
    new AbstractActor.Receive(
      apply(r.onMessage.asInstanceOf[PartialFunction[Any, Unit]])(context, adapter)
        .asInstanceOf[PartialFunction[Any, BoxedUnit]])

  def withLabel[FB <: AkkaFieldBuilder](label: String, logLevel: LogLevel)(r: Receive)(implicit context: ActorContext, adapter: EchopraxiaLoggingAdapter[FB]): Receive = r match {
    case _: LoggingReceive[_] => r
    case _ => if (context.system.settings.AddLoggingReceive) new LoggingReceive(r, Option(label), logLevel) else r
  }

  def withLabel[FB <: AkkaFieldBuilder](label: String)(r: Receive)(implicit context: ActorContext, adapter: EchopraxiaLoggingAdapter[FB]): Receive =
    withLabel(label, Logging.DebugLevel)(r)
}

class LoggingReceive[FB <: AkkaFieldBuilder](r: Receive, label: Option[String], logLevel: LogLevel)(
  implicit context: ActorContext, adapter: EchopraxiaLoggingAdapter[FB])
  extends Receive {
  def this(r: Receive, label: Option[String])(implicit context: ActorContext, adapter: EchopraxiaLoggingAdapter[FB]) =
    this(r, label, Logging.DebugLevel)(context, adapter)

  def this(r: Receive)(implicit context: ActorContext, adapter: EchopraxiaLoggingAdapter[FB]) =
    this(r, None, Logging.DebugLevel)(context, adapter)

  def isDefinedAt(o: Any): Boolean = {
    val handled = r.isDefinedAt(o)
    if (context.system.eventStream.logLevel >= logLevel) {
      val message = "received {} message {} from " +
        (label match {
          case Some(l) => " in state " + l
          case _ => ""
        })


      val fbf: FB => FieldBuilderResult = { fb: FB =>
        import fb._
        fb.list(
          fb.keyValue("handled" -> handled),
          fb.string("message", Objects.toString(o)),
          fb.value("sender", context.sender()),
        )
      }

      adapter.core.log(Level.valueOf(logLevel.toString), message, fbf.asJava, adapter.fieldBuilder)
    }
    handled
  }

  def apply(o: Any): Unit = r(o)
}