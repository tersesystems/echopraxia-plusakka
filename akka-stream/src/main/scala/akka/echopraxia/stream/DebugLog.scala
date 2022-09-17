package akka.echopraxia.stream

import akka.event._
import akka.stream.ActorAttributes.SupervisionStrategy
import akka.stream.Supervision.Decider
import akka.stream._
import akka.stream.impl.fusing.GraphStages.SimpleLinearGraphStage
import akka.stream.stage._
import com.tersesystems.echopraxia.api.Field
import com.tersesystems.echopraxia.plusscala.Logger

import scala.util.control.NonFatal

final case class DebugLog[FB <: AkkaStreamFieldBuilder, T](
  name: String,
  extract: (FB, T) => Field,
  nameKey: String,
  operationKey: String,
  causeKey: String,
  logAdapter: EchopraxiaLoggingAdapter[FB]
) extends SimpleLinearGraphStage[T] {

  override def toString = "DebugLog"

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) with OutHandler with InHandler {
      def decider: Decider = inheritedAttributes.mandatoryAttribute[SupervisionStrategy].decider

      // LogLevels(onElement = Logging.DebugLevel, onFinish = Logging.DebugLevel, onFailure = Logging.ErrorLevel)
      var log: Logger[FB] = _
      override def preStart(): Unit = {
        log = logAdapter.getLogger()(materializer)
      }

      override def onPush(): Unit = {
        try {
          val elem = grab(in)
          log.debug("[{}]: {} {}", fb => fb.list(
            fb.string(nameKey, name),
            fb.string(operationKey, "push"),
            extract(fb, elem)
          ))

          push(out, elem)
        } catch {
          case NonFatal(ex) =>
            decider(ex) match {
              case Supervision.Stop => failStage(ex)
              case _ => pull(in)
            }
        }
      }

      override def onPull(): Unit = pull(in)

      override def onUpstreamFailure(cause: Throwable): Unit = {
        log.error(
          "[{}]: {} {} {}", fb => fb.list(
            fb.string(nameKey, name),
            fb.string(operationKey, "upstreamFailure"),
            fb.keyValue(causeKey, Logging.simpleName(cause.getClass)),
            fb.exception(cause)
          )
         )

        super.onUpstreamFailure(cause)
      }

      override def onUpstreamFinish(): Unit = {
        log.debug("[{}]: {}", fb => fb.list(
          fb.string(nameKey, name),
          fb.string(operationKey, "upstreamFinish")))
        super.onUpstreamFinish()
      }

      override def onDownstreamFinish(cause: Throwable): Unit = {
          log.debug(
            "[{}]: {} {}: {}",
            fb => fb.list(
              fb.string(nameKey, name),
              fb.string(operationKey, "downstreamFinish"),
              fb.keyValue(causeKey, Logging.simpleName(cause.getClass)),
              fb.exception(cause)
            ))

        super.onDownstreamFinish(cause: Throwable)
      }
      setHandlers(in, out, this)
    }

}

object DebugLog {
  val NameKey = "name"

  val CauseKey = "cause"

  val OperationKey = "operation"
}
