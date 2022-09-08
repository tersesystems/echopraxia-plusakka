package akka.echopraxia.actor.typed

import akka.actor.{Address, ClassicActorContextProvider}
import akka.actor.typed.internal.adapter.ActorSystemAdapter
import akka.echopraxia.actor.{AkkaFieldBuilder, DefaultAkkaFieldBuilder}

trait AkkaTypedFieldBuilder extends AkkaFieldBuilder {
  implicit def signalToValue: ToValue[akka.actor.typed.Signal]

  implicit def typedActorRefToValue[T]: ToValue[akka.actor.typed.ActorRef[T]]

  implicit def typedActorContextRefToValue[T]: ToValue[akka.actor.typed.scaladsl.ActorContext[T]]

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

  implicit def typedActorContextRefToValue[T]: ToValue[akka.actor.typed.scaladsl.ActorContext[T]] = { ctx =>
    // We want something that matches LoggerContext, but that's private
    val akkaSource = ctx.self.path.toString
    val akkaAddress =
      ctx.system match {
        case adapter: ActorSystemAdapter[_] => adapter.provider.addressString
        case _                              => Address("akka", ctx.system.name).toString
      }
    val sourceActorSystem = ctx.system.name
    val tags = ctx match {
      case p: ClassicActorContextProvider =>
        p.classicActorContext.props.deploy.tags
      case _ =>
        Set.empty
    }
    ToObjectValue(
      keyValue("tags", tags),
      keyValue("akkaSource", akkaSource),
      keyValue("sourceActorSystem", sourceActorSystem),
      keyValue("akkaAddress", akkaAddress)
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