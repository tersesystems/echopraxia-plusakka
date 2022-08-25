package com.tersesystems.echopraxia.plusakka

import akka.actor.typed.{ActorSystem, Signal}
import akka.actor.{ActorPath, Address}
import com.tersesystems.echopraxia.api.Field
import com.tersesystems.echopraxia.plusscala.api.FieldBuilder

trait AkkaFieldBuilder extends FieldBuilder {
  implicit def addressToValue: ToValue[Address]
  implicit def typedActorRefToValue[T]: ToValue[akka.actor.typed.ActorRef[T]]
  implicit def actorRefToValue: ToValue[akka.actor.ActorRef]
  implicit def actorPathToValue: ToValue[ActorPath]

  implicit def signalToValue: ToValue[Signal]

  implicit def typedActorSystemToValue[T]: ToValue[akka.actor.typed.ActorSystem[T]]
  implicit def actorSystemToValue: ToValue[akka.actor.ActorSystem]

  def actorSystem(name: String, actorSystem: akka.actor.ActorSystem): Field = {
    keyValue(name, actorSystem)
  }

  def actorRef(name: String, actorRef: akka.actor.ActorRef): Field = {
    keyValue(name, actorRef)
  }

}

trait DefaultAkkaFieldBuilder extends AkkaFieldBuilder with FieldBuilder {
  implicit val addressToValue: ToValue[Address] = { address =>
    ToValue(address.toString)
  }

  implicit def typedActorRefToValue[T]: ToValue[akka.actor.typed.ActorRef[T]] = { actorRef =>
    ToObjectValue(
      keyValue("path" -> actorRef.path),
      keyValue("uid" -> actorRef.hashCode)
    )
  }

  implicit val actorRefToValue: ToValue[akka.actor.ActorRef] = { actorRef =>
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

  implicit def typedActorSystemToValue[T]: ToValue[akka.actor.typed.ActorSystem[T]] = { actorSystem =>
    ToObjectValue(
      keyValue("system" -> actorSystem.name),
      keyValue("startTime" -> actorSystem.startTime),
    )
  }

  implicit def actorSystemToValue: ToValue[akka.actor.ActorSystem] = { actorSystem =>
    ToObjectValue(
      keyValue("system" -> actorSystem.name),
      keyValue("startTime" -> actorSystem.startTime),
    )
  }

}

object DefaultAkkaFieldBuilder extends DefaultAkkaFieldBuilder