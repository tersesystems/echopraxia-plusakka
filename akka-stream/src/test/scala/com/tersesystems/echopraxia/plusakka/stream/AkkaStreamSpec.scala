package com.tersesystems.echopraxia.plusakka.stream

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.echopraxia.stream.DefaultAkkaStreamFieldBuilder
import akka.event.{Logging, LoggingAdapter}
import akka.stream.Attributes
import akka.stream.scaladsl._
import akka.stream.testkit.TestSubscriber
import akka.stream.testkit.scaladsl._
import akka.testkit.TestKit
import com.tersesystems.echopraxia.plusscala.LoggerFactory
import com.tersesystems.echopraxia.plusscala.api.FieldBuilder
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class AkkaStreamSpec extends TestKit(ActorSystem("MySpec")) with AnyWordSpecLike with Matchers with BeforeAndAfterAll {

  private val logger = LoggerFactory.getLogger.withFieldBuilder(DefaultAkkaStreamFieldBuilder)

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "testing" in {
    DoThing().runWith(TestSink[Int]()).request(2).expectNext(4, 8).expectComplete()
  }

  object DoThing extends FieldBuilder with HasLoggingAdapter {
    override implicit val loggingAdapter: LoggingAdapter = fromSystem

    private val source: Source[Int, NotUsed] = Source(1 to 4).filter(_ % 2 == 0)
      .log("before", keyValue("element", _))
      .map(_ * 2)
      .log("after", keyValue("element", _))

    def apply(): Source[Int, NotUsed] = source
  }
}

trait HasLoggingAdapter {
  implicit val loggingAdapter: LoggingAdapter

  def fromSystem(implicit system: ActorSystem): LoggingAdapter = Logging.getLogger(system, this)
}

