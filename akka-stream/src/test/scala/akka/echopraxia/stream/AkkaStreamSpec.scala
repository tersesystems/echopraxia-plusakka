package akka.echopraxia.stream

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.echopraxia.actor.Implicits._
import akka.echopraxia.stream.DefaultAkkaStreamFieldBuilder
import akka.echopraxia.stream.Implicits.SourceLogging
import akka.event.{LogSource, Logging, LoggingAdapter}
import akka.stream.Materializer
import akka.stream.scaladsl._
import akka.stream.testkit.scaladsl._
import akka.testkit.TestKit
import com.tersesystems.echopraxia.plusscala.{Logger, LoggerFactory}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class AkkaStreamSpec extends TestKit(ActorSystem("MySpec")) with AnyWordSpecLike with Matchers with BeforeAndAfterAll {

  private val logger: Logger[DefaultAkkaStreamFieldBuilder.type] = LoggerFactory.getLogger.withFieldBuilder(DefaultAkkaStreamFieldBuilder)

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "testing" in {
    DoThing().runWith(TestSink[Int]()).request(2).expectNext(4, 8).expectComplete()
  }

  object DoThing {
    private implicit val loggingAdapter = EchopraxiaLoggingAdapter.fromLogger(logger)
    private val source: Source[Int, NotUsed] = Source(1 to 4).filter(_ % 2 == 0)
      .log2.debug("before", _.keyValue("elem", _))
      .map(_ * 2)
      .log2.debug("after", _.keyValue("elem", _))

    def apply(): Source[Int, NotUsed] = source
  }
}
