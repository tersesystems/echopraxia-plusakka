package akka.echopraxia.actor

import akka.actor.{AllForOneStrategy, OneForOneStrategy, Scope}
import akka.routing.RouterConfig
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

  implicit def propsToValue: ToValue[akka.actor.Props]

  implicit def deployToValue: ToValue[akka.actor.Deploy]

  implicit def scopeToValue: ToValue[Scope]

  implicit def routerConfigToValue: ToValue[RouterConfig]

  // don't do status, it returns Any which breaks the chain
  // implicit def statusToValue: ToValue[Status]

  implicit def supervisorStrategyToValue: ToValue[akka.actor.SupervisorStrategy]

}

trait DefaultAkkaFieldBuilder extends AkkaFieldBuilder {

  override implicit val doneToValue: ToValue[Done] = _ => ToValue(Done.toString)

  override implicit val notUsedToValue: ToValue[NotUsed] = _ => ToValue(NotUsed.toString)

  override implicit val addressToValue: ToValue[akka.actor.Address] = { address =>
    ToValue(address.toString)
  }

  override implicit val actorRefToValue: ToValue[akka.actor.ActorRef] = { actorRef =>
    ToValue(keyValue("path" -> actorRef.path))
  }

  override implicit val actorPathToValue: ToValue[akka.actor.ActorPath] = { actorPath =>
    ToObjectValue(
      keyValue("address" -> actorPath.address),
      keyValue("name" -> actorPath.name),
      keyValue("uid" -> actorPath.uid)
    )
  }

  /* Java API */
  def keyValue(name: String, actorPath: akka.actor.ActorPath): Field = keyValue(name, ToValue(actorPath))

  override implicit def actorSystemToValue: ToValue[akka.actor.ActorSystem] = { actorSystem =>
    ToObjectValue(
      keyValue("system" -> actorSystem.name),
      keyValue("startTime" -> actorSystem.startTime),
    )
  }

  override implicit def propsToValue: ToValue[akka.actor.Props] = { props =>
    ToObjectValue(
      keyValue("deploy" -> props.deploy),
      keyValue("clazz" -> props.clazz.getName),
      keyValue("args" -> props.args.map(_.toString))
    )
  }

  override implicit def deployToValue: ToValue[akka.actor.Deploy] = { deploy =>
    ToObjectValue(
      keyValue("routerConfig" -> deploy.routerConfig),
      keyValue("path" -> deploy.path),
      keyValue("tags" -> deploy.tags),
      keyValue("scope" -> deploy.scope),
      keyValue("dispatcher" -> deploy.dispatcher),
    )
  }

  override implicit def scopeToValue: ToValue[Scope] = { scope =>
    ToValue(scope.toString)
  }

  implicit def routerConfigToValue: ToValue[RouterConfig] = { routerConfig =>
    ToValue(routerConfig.toString)
  }

  override implicit def supervisorStrategyToValue: ToValue[akka.actor.SupervisorStrategy] = {
    case c@AllForOneStrategy(maxNrOfRetries, withinTimeRange, _) =>
      ToObjectValue(
        keyValue("className" -> c.getClass.getName),
        keyValue("maxNrOfRetries" -> maxNrOfRetries),
        keyValue("withinTimeRange" -> withinTimeRange.toString)
      )
    case c@OneForOneStrategy(maxNrOfRetries, withinTimeRange, _) =>
      ToObjectValue(
        keyValue("className" -> c.getClass.getName),
        keyValue("maxNrOfRetries" -> maxNrOfRetries),
        keyValue("withinTimeRange" -> withinTimeRange.toString)
      )
    case other =>
      ToValue(
        keyValue("className" -> other.getClass.getName),
      )
  }

}

object DefaultAkkaFieldBuilder extends DefaultAkkaFieldBuilder