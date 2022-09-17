package example

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import akka.echopraxia.actor.typed.DefaultAkkaTypedFieldBuilder
import akka.echopraxia.actor.typed.Implicits._
import ch.qos.logback.classic.LoggerContext
import com.tersesystems.echopraxia.plusscala._

import scala.concurrent.duration._

object Main {

  trait Command
  case object Tick extends Command
  case class Echo(message: String) extends Command

  def main(args: Array[String]): Unit = {
    // Stop the warnings by explicitly managing logger context
    val loggerContext: LoggerContext = org.slf4j.LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
    loggerContext.start()
    val system = ActorSystem(MyActor(), "hello")
    system.whenTerminated.map { _ =>
      loggerContext.stop()
    }(scala.concurrent.ExecutionContext.global)
  }

  object MyActor {
    def apply(): Behavior[Tick.type] = Behaviors.setup { context =>
      Behaviors.withTimers { timers =>
        val echo = context.spawn(EchoActor(), "echo")
        timers.startTimerWithFixedDelay(Tick, 1.seconds)
        Behaviors.receiveMessage {
          case Tick =>
            echo ! Echo(java.time.Instant.now().toString)
            Behaviors.same
        }
      }
    }
  }

  trait MyFieldBuilder extends DefaultAkkaTypedFieldBuilder {
    implicit val commandToValue: ToValue[Command] = cmd => ToValue(cmd.toString)
  }
  object MyFieldBuilder extends MyFieldBuilder

  object EchoActor {
    def apply(): Behavior[Echo] = Behaviors.setup { context =>
      val logger = LoggerFactory.getLogger.withFieldBuilder(MyFieldBuilder).withActorContext(context)

      Behaviors.receiveMessage { echo =>
        logger.info("echoActor: {}", _.keyValue("echo", echo))
        Behaviors.same
      }
    }
  }
}
