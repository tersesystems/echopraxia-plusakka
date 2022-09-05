package example

import akka.actor.typed._
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import com.tersesystems.echopraxia.logback.ConditionMarker
import com.tersesystems.echopraxia.plusakka.actor.typed.Implicits._
import com.tersesystems.echopraxia.plusscala.LoggerFactory
import com.tersesystems.echopraxia.plusscala.api.Condition

object HelloWorld extends HelloWorldFieldBuilder {

  private val logger = LoggerFactory.getLogger.withFieldBuilder(this)

  private val condition = Condition.always

  def apply(): Behavior[Greet] = logger.debugMessages[Greet] {
    Behaviors.receive { (context, message) =>
      context.log.info(ConditionMarker.apply(condition.asJava), "Received message: {}", keyValue("foo", message))
      message.replyTo ! Greeted(message.whom, context.self)
      Behaviors.same
    }
  }
}

object HelloWorldBot extends HelloWorldFieldBuilder {

  private val logger = LoggerFactory.getLogger.withFieldBuilder(this)

  def apply(max: Int): Behavior[Greeted] = {
    bot(0, max)
  }

  private def bot(greetingCounter: Int, max: Int): Behavior[Greeted] = {
    logger.debugMessages[Greeted] {
      Behaviors.receive { (context, message) =>
        val n = greetingCounter + 1
        context.log.info(s"Greeting $n for {}", keyValue("message" -> message.whom))
        if (n == max) {
          Behaviors.stopped
        } else {
          message.from ! Greet(message.whom, context.self)
          bot(n, max)
        }
      }
    }
  }
}

object HelloWorldMain extends HelloWorldFieldBuilder {
  final case class SayHello(name: String)

  def apply(): Behavior[SayHello] =
    Behaviors.setup { context =>
      val greeter = context.spawn(HelloWorld(), "greeter")
        Behaviors.receiveMessage { message =>
          val replyTo = context.spawn(HelloWorldBot(max = 3), message.name)
          greeter ! Greet(message.name, replyTo)
          Behaviors.same
        }
    }

  def main(args: Array[String]): Unit = {
    val system: ActorSystem[HelloWorldMain.SayHello] =
      ActorSystem(HelloWorldMain(), "hello")

    system ! HelloWorldMain.SayHello("World")
    system ! HelloWorldMain.SayHello("Akka")
  }

  // Need to work with context
  Behaviors.setup[String] { context: ActorContext[String] =>
    context.setLoggerName("com.myservice.BackendManager")

    context.log.info("Starting up")

    Behaviors.receiveMessage { message =>
      context.log.debug("Received message: {}", message)
      Behaviors.same
    }
  }
}

final case class Greet(whom: String, replyTo: ActorRef[Greeted])

final case class Greeted(whom: String, from: ActorRef[Greet])
