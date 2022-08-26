package com.tersesystems.echopraxia.plusakka.actor.typed

import com.tersesystems.echopraxia.plusakka.actor.{AkkaFieldBuilder, DefaultAkkaFieldBuilder}

trait AkkaTypedFieldBuilder extends AkkaFieldBuilder {
  implicit def signalToValue: ToValue[akka.actor.typed.Signal]

  implicit def typedActorRefToValue[T]: ToValue[akka.actor.typed.ActorRef[T]]

  implicit def typedActorSystemToValue[T]: ToValue[akka.actor.typed.ActorSystem[T]]
}

trait DefaultAkkaTypedFieldBuilder extends DefaultAkkaFieldBuilder with AkkaTypedFieldBuilder {

  override implicit val signalToValue: ToValue[akka.actor.typed.Signal] = { signal =>
    ToValue(signal.toString)
  }

  override implicit def typedActorRefToValue[T]: ToValue[akka.actor.typed.ActorRef[T]] = { actorRef =>
    ToObjectValue(
      keyValue("path" -> actorRef.path),
      keyValue("uid" -> actorRef.hashCode)
    )
  }

  override implicit def typedActorSystemToValue[T]: ToValue[akka.actor.typed.ActorSystem[T]] = { actorSystem =>
    ToObjectValue(
      keyValue("system" -> actorSystem.name),
      keyValue("startTime" -> actorSystem.startTime),
    )
  }

}

object DefaultAkkaTypedFieldBuilder extends DefaultAkkaTypedFieldBuilder