package example

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, ActorTestKitBase, LogCapturing, LoggingTestKit, ScalaTestWithActorTestKit}
import akka.actor.typed.ActorSystem
import ch.qos.logback.classic.LoggerContext
import example.Main.{Echo, EchoActor}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

class MainSpec extends AnyWordSpecLike with LogCapturing with BeforeAndAfterAll {

  private val testKit = ActorTestKit(ActorTestKitBase.testNameFromCallStack())

  implicit def system: ActorSystem[Nothing] = testKit.system

  override def beforeAll(): Unit = {
    val loggerContext: LoggerContext = org.slf4j.LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
    loggerContext.start()
    super.beforeAll()
  }

  "Something" must {
    "behave correctly" in {
      val echoActor = testKit.spawn(EchoActor(), "ping")
      LoggingTestKit.info("echoActor: echo=Echo(Received message)").expect {
        echoActor ! Echo("Received message")
      }
    }
  }
}