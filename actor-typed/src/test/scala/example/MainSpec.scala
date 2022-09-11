package example

import akka.actor.testkit.typed.scaladsl.{LogCapturing, LoggingTestKit, ScalaTestWithActorTestKit}
import example.Main.{Echo, EchoActor}
import org.scalatest.wordspec.AnyWordSpecLike


class MainSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike with LogCapturing {

  "Something" must {
    "behave correctly" in {
      val echoActor = testKit.spawn(EchoActor(), "ping")
      LoggingTestKit.info("echoActor: echo=Echo(Received message)").expect {
        echoActor ! Echo("Received message")
      }
    }
  }
}