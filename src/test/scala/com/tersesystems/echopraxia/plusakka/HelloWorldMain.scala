package com.tersesystems.echopraxia.plusakka

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed._

import Implicits._
import com.tersesystems.echopraxia.plusscala.LoggerFactory

object HelloWorld {

  final case class Greet(whom: String, replyTo: ActorRef[Greeted])

  final case class Greeted(whom: String, from: ActorRef[Greet])

  private val logger = LoggerFactory.getLogger.withFieldBuilder(HelloWorldFieldBuilder)

  def apply(): Behavior[Greet] = logger.logMessages[Greet](
    Behaviors.receive { (context, message) =>
      println(s"Hello ${message.whom}!")
      message.replyTo ! Greeted(message.whom, context.self)
      Behaviors.same
    }
  )
}

object HelloWorldBot {

  private val logger = LoggerFactory.getLogger.withFieldBuilder(HelloWorldFieldBuilder)

  def apply(max: Int): Behavior[HelloWorld.Greeted] = {
    bot(0, max)
  }

  private def bot(greetingCounter: Int, max: Int): Behavior[HelloWorld.Greeted] = {
    logger.logMessages[HelloWorld.Greeted]({
      Behaviors.receive { (context, message) =>
        val n = greetingCounter + 1
        logger.info(s"Greeting $n for {}", _.keyValue("message" -> message.whom))
        if (n == max) {
          Behaviors.stopped
        } else {
          message.from ! HelloWorld.Greet(message.whom, context.self)
          bot(n, max)
        }
      }
    })
  }
}

object HelloWorldMain {
  final case class SayHello(name: String)

  private val logger = LoggerFactory.getLogger.withFieldBuilder(HelloWorldFieldBuilder)

  def apply(): Behavior[SayHello] =
    Behaviors.setup { context =>
      val greeter = context.spawn(HelloWorld(), "greeter")

      val fromName = LoggerFactory.getLogger(context.log.getName).withFieldBuilder(DefaultAkkaFieldBuilder)

      logger.logMessages[SayHello] {
        Behaviors.receiveMessage { message =>
          val replyTo = context.spawn(HelloWorldBot(max = 3), message.name)
          greeter ! HelloWorld.Greet(message.name, replyTo)
          Behaviors.same
        }
      }
    }

  def main(args: Array[String]): Unit = {
    val system: ActorSystem[HelloWorldMain.SayHello] =
      ActorSystem(HelloWorldMain(), "hello")

    system ! HelloWorldMain.SayHello("World")
    system ! HelloWorldMain.SayHello("Akka")
  }
}
