package akka.echopraxia.actor

import akka.actor.ActorSystem
import com.tersesystems.echopraxia.plusscala.Logger

trait Implicits {

  implicit class AkkaLoggerOps[FB <: AkkaFieldBuilder](logger: Logger[FB]) {

    def withActorSystem(actorSystem: ActorSystem): Logger[FB] = {
      val fb = logger.fieldBuilder
      val actorSystemField = fb.keyValue("system" -> actorSystem)(fb.actorSystemToValue)
      logger.withFields(_ => actorSystemField)
    }

  }

}

object Implicits extends Implicits