package akka.echopraxia.actor

import akka.actor.Actor
import com.tersesystems.echopraxia.plusscala.{Logger, LoggerFactory}

trait ActorLogging {
  this: Actor with AkkaFieldBuilderProvider =>
  protected val log: Logger[FieldBuilderType] = LoggerFactory.getLogger
    .withThreadContext
    .withFieldBuilder(fieldBuilder)
    .withFields(fb => {
      import fb._
      fb.list(
        fb.keyValue("system" -> context.system),
        fb.keyValue("self" -> self),
      )
    })
}
