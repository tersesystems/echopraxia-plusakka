package akka.echopraxia.actor

import akka.{Done, NotUsed}
import com.tersesystems.echopraxia.api.Field
import com.tersesystems.echopraxia.plusscala.api.FieldBuilder

trait AkkaFieldBuilder extends FieldBuilder {

  implicit def doneToValue: ToValue[Done]

  implicit def notUsedToValue: ToValue[NotUsed]

  implicit def addressToValue: ToValue[akka.actor.Address]
  implicit def actorRefToValue: ToValue[akka.actor.ActorRef]
  implicit def actorPathToValue: ToValue[akka.actor.ActorPath]

  implicit def actorSystemToValue: ToValue[akka.actor.ActorSystem]

  def actorSystem(name: String, actorSystem: akka.actor.ActorSystem): Field = {
    keyValue(name, actorSystem)
  }

  def actorRef(name: String, actorRef: akka.actor.ActorRef): Field = {
    keyValue(name, actorRef)
  }

}

trait DefaultAkkaFieldBuilder extends AkkaFieldBuilder {

  override implicit val doneToValue: ToValue[Done] = _ => ToValue(Done.toString)

  override implicit val notUsedToValue: ToValue[NotUsed] = _ => ToValue(NotUsed.toString)

  override implicit val addressToValue: ToValue[akka.actor.Address] = { address =>
    ToValue(address.toString)
  }

  override implicit val actorRefToValue: ToValue[akka.actor.ActorRef] = { actorRef =>
    ToObjectValue(
      keyValue("path" -> actorRef.path),
      keyValue("uid" -> actorRef.hashCode)
    )
  }

  override implicit val actorPathToValue: ToValue[akka.actor.ActorPath] = { actorPath =>
    ToValue(actorPath.toString)
  }

  override implicit def actorSystemToValue: ToValue[akka.actor.ActorSystem] = { actorSystem =>
    ToObjectValue(
      keyValue("system" -> actorSystem.name),
      keyValue("startTime" -> actorSystem.startTime),
    )
  }

}

object DefaultAkkaFieldBuilder extends DefaultAkkaFieldBuilder