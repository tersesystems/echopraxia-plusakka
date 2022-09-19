package akka.echopraxia.stream

import akka.event._
import akka.stream.ActorAttributes.SupervisionStrategy
import akka.stream.Supervision.Decider
import akka.stream._
import akka.stream.impl.fusing.GraphStages.SimpleLinearGraphStage
import akka.stream.stage._
import com.tersesystems.echopraxia.api.{CoreLogger, Field, Level}

import scala.util.control.NonFatal

final case class Log2[FB <: AkkaStreamFieldBuilder, T](
  level: Level,
  name: String,
  extract: (FB, T) => Field,
  nameKey: String,
  operationKey: String,
  causeKey: String,
  core: CoreLogger,
  fieldBuilder: FB
) extends SimpleLinearGraphStage[T] {

  override def toString = "Log2"

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) with OutHandler with InHandler {
      def decider: Decider = inheritedAttributes.mandatoryAttribute[SupervisionStrategy].decider

      var log: CoreLogger = _
      override def preStart(): Unit = {
        val frozenMatPath = fieldBuilder.keyValue("materializer", materializer.supervisor.path)(fieldBuilder.actorPathToValue)
        log = core.withFields((_: FB) => frozenMatPath, fieldBuilder)
      }

      override def onPush(): Unit = {
        try {
          val elem = grab(in)
          log.log(level, "[{}]: {} {}", (fb: FB) => fb.list(
            fb.string(nameKey, name),
            fb.string(operationKey, "push"),
            extract(fb, elem)
          ), fieldBuilder)

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
        log.log(Level.ERROR,
          "[{}]: {} {} {}", (fb: FB) => fb.list(
            fb.string(nameKey, name),
            fb.string(operationKey, "upstreamFailure"),
            fb.keyValue(causeKey, Logging.simpleName(cause.getClass)),
            fb.exception(cause)
          ), fieldBuilder
         )

        super.onUpstreamFailure(cause)
      }

      override def onUpstreamFinish(): Unit = {
        log.log(level, "[{}]: {}", (fb: FB) => fb.list(
          fb.string(nameKey, name),
          fb.string(operationKey, "upstreamFinish")), fieldBuilder)
        super.onUpstreamFinish()
      }

      override def onDownstreamFinish(cause: Throwable): Unit = {
          log.log(level,
            "[{}]: {} {}: {}",
            (fb: FB) => fb.list(
              fb.string(nameKey, name),
              fb.string(operationKey, "downstreamFinish"),
              fb.keyValue(causeKey, Logging.simpleName(cause.getClass)),
              fb.exception(cause)
            ), fieldBuilder)

        super.onDownstreamFinish(cause: Throwable)
      }
      setHandlers(in, out, this)
    }

}

object Log2 {
  val NameKey = "name"

  val CauseKey = "cause"

  val OperationKey = "operation"
}
