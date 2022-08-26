package com.tersesystems.echopraxia.plusakka

import akka.actor.Actor
import com.tersesystems.echopraxia.plusscala.{Logger, LoggerFactory}

trait ActorLogging { this: Actor with FieldBuilderProvider =>
  protected val log: Logger[FieldBuilderType] = LoggerFactory.getLogger
    .withThreadContext
    .withFieldBuilder(fieldBuilder)
    .withFields(fb => {
      import fb._
      list(
        keyValue("system" -> context.system),
        keyValue("self" -> self),
      )
    })
}
