package com.tersesystems.echopraxia.plusakka

import akka.actor.typed.{ActorRef, Signal}
import akka.actor.{ActorPath, Address}
import com.tersesystems.echopraxia.plusscala.api.FieldBuilder

trait AkkaFieldBuilder extends FieldBuilder {
  implicit val addressToValue: ToValue[Address] = { address =>
    ToValue(address.toString)
  }

  implicit def actorRefToValue[T]: ToValue[ActorRef[T]] = { actorRef =>
    ToObjectValue(
      keyValue("path" -> actorRef.path),
      keyValue("uid" -> actorRef.hashCode)
    )
  }

  implicit val actorPathToValue: ToValue[ActorPath] = { actorPath =>
    ToValue(actorPath.toString)
  }

  implicit val signalToValue: ToValue[Signal] = { signal =>
    ToValue(signal.toString)
  }
}

object AkkaFieldBuilder extends AkkaFieldBuilder