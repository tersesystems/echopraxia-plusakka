package akka.echopraxia.stream

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.echopraxia.stream.Implicits._
import akka.stream.scaladsl._
import akka.stream.testkit.scaladsl._
import akka.testkit.TestKit
import com.tersesystems.echopraxia.plusscala.api.Condition
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class AkkaStreamSpec extends TestKit(ActorSystem("MySpec")) with AnyWordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "testing" in {
    DoThing().runWith(TestSink[Int]()).request(2).expectNext(4, 8).expectComplete()
  }

  //val condition = Condition(lc => lc.findNumber("$.elem").exists(p => p.intValue > 2))
  val condition = Condition.always

  object DoThing {
    private implicit val loggingAdapter = EchopraxiaLoggingAdapter(this.getClass, DefaultAkkaStreamFieldBuilder)

    private val source: Source[Int, NotUsed] = Source(1 to 4).filter(_ % 2 == 0)
      .log2.withCondition(condition).withFields(fb => fb.keyValue("foo", "bar")).info("before", (fb, el) => fb.keyValue("elem", el))
      .map(_ * 2)
      .log2.debug("after", (fb, el) => fb.keyValue("elem", el))

    def apply(): Source[Int, NotUsed] = source
  }
}
