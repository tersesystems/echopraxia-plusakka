# Echopraxia Plus Akka

This library provides Akka specific functionality for integrating [Echopraxia](https://github.com/tersesystems/echopraxia) structured logging into [Akka](https://akka.io).

This README assumes that you have `akka-slf4j` set up and have logging configured appropriately in Akka, and have the [Scala API](https://github.com/tersesystems/echopraxia-plusscala) in scope.

## Akka Actors

Adding Echopraxia Logging to Akka classic actors takes a little setup, but is hassle-free. 

There is an `AkkaFieldBuilder` trait that provides field builder mappings for common class `akka.actor.Address`, `akka.actor.ActorRef`, etc.

The `DefaultAkkaFieldBuilder` implementation fills out those mappings, and there's a `DefaultAkkaFieldBuilderProvider` that exposes that field builder.

```scala
package akka.echopraxia.actor

trait DefaultAkkaFieldBuilderProvider extends AkkaFieldBuilderProvider {
  override type FieldBuilderType = DefaultAkkaFieldBuilder.type
  override protected def fieldBuilder: FieldBuilderType = DefaultAkkaFieldBuilder
}
```

The trait `ActorLogging` provides a logger, and wants a `AkkaFieldBuilderProvider` trait to be mixed in:

```scala
package akka.echopraxia.actor

trait ActorLogging {
  this: Actor with AkkaFieldBuilderProvider =>
  protected val log: Logger[FieldBuilderType] = ...
}
```

This means that at the end of it, you can add `ActorLogging with DefaultAkkaFieldBuilderProvider` to your actor, and it will log appropriately.

```scala
import akka.echopraxia.actor._

class MyActor extends Actor with ActorLogging with DefaultAkkaFieldBuilderProvider {

  override def preStart() = {
    log.debug("Starting")
  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    log.error("Restarting due to [{}] when processing [{}]", fb => fb.list(
      fb.exception(reason),
      fb.string("message" -> message.toString),
      fb.keyValue("self" -> self.path)
    ))
  }

  def receive = {
    case "test" => log.info("Received test")
    case x      => log.warn("Received unknown message: {}", _.string("x" -> x.toString))
  }
}
```

## Akka Typed Logging

Echopraxia logging for Akka Typed is similar to [typed logging](https://doc.akka.io/docs/akka/current/typed/logging.html).

There is an `AkkaTypedFieldBuilder` trait that requires `ToValue` for Akka Typed classes like `akka.actor.typed.ActorRef[T]`, and a `DefaultAkkaTypedFieldBuilder` trait and companion object that provides the default mappings.

Typically, you'll create your own field builder by extending these traits.  For example, using the [hello world example](https://doc.akka.io/docs/akka/current/typed/actor-lifecycle.html#creating-actors) from the docs:

```scala
trait HelloWorldFieldBuilder extends DefaultAkkaTypedFieldBuilder {
  implicit val greetToValue: ToObjectValue[Greet] = { greet: Greet =>
    ToObjectValue(
      keyValue("whom", greet.whom),
      keyValue("replyTo", greet.replyTo)
    )
  }
  // ...
}
object HelloWorldFieldBuilder extends HelloWorldFieldBuilder
```

Akka Typed incorporates the context through behaviors, using a context.  This context is added to `context.log` as MDC, but you can add it directly as context fields to the logger using `withFields`.  This is call by name, so you can optimize it by freezing it in `Behaviors.setup`:

```scala
package example

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import akka.echopraxia.actor.typed.DefaultAkkaTypedFieldBuilder
import com.tersesystems.echopraxia.plusscala.LoggerFactory

import scala.concurrent.duration._

object Main {

  trait Command
  case object Tick extends Command
  case class Echo(message: String) extends Command

  def main(args: Array[String]): Unit = {
    val system = ActorSystem(MyActor(), "hello")
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
      val frozenContext = MyFieldBuilder.keyValue("context" -> context)
      val logger = LoggerFactory.getLogger
        .withFieldBuilder(MyFieldBuilder)
        .withFields(_ => frozenContext) // call-by-name

      Behaviors.receiveMessage { echo =>
        logger.info("echoActor: {}", _.keyValue("echo", echo))
        Behaviors.same
      }
    }
  }
}
```

You can also use the "Direct SLF4J API" from Echopraxia to pass through conditions and fields through markers, and arguments can be passed through the field builder interface as usual.

```scala
object HelloWorld extends HelloWorldFieldBuilder {
  def apply(): Behavior[Greet] = {
    Behaviors.receive { (context, message) =>
      // keyValue(name, ToValue) is provided by field builder API 
      // context.log will set MDC values when called for the thread.
      context.log.info("Received message: {}", keyValue("foo", message))
      message.replyTo ! Greeted(message.whom, context.self)
      Behaviors.same
    }
  }
}
```

**Note**: Echopraxia filters will not go through the direct API, so if you have "global" conditions or fields that you want to apply to all loggers, they will need to be added directly to `context.log`, or you'll need to add a Logback filter or turbo filter.

### Alternative to Behaviors.logMessages

The `akka.echopraxia.actor.typed.Implicits` trait contains `AkkaLoggerOps`, which has `logger.debugMessages`.  This can be used as an alternative to `Behaviors.logMessages` [logging](https://doc.akka.io/docs/akka/current/typed/logging.html#behaviors-logmessages).

```scala
import akka.echopraxia.actor.typed.Implicits._

val logger = LoggerFactory.getLogger.withFieldBuilder(HelloWorldFieldBuilder)

def apply(): Behavior[Greet] = logger.debugMessages[Greet] {
  Behaviors.receive { (context, message) =>
    message.replyTo ! Greeted(message.whom, context.self)
    Behaviors.same
  }
}
```

## Akka Streams

There are two main reasons why Akka Streams should always incorporate logging of some sort.

The first reason is that by default, Akka Streams will [swallow exceptions](https://blog.softwaremill.com/akka-streams-pitfalls-to-avoid-part-1-75ef6403c6e6) and not log them.  As such, you need to have a `log` or `recover` op to make the exception visible.

The second reason is that logging is the [best way](https://blog.softwaremill.com/akka-streams-pitfalls-to-avoid-part-2-f93e60746c58) to see what's happening in a stream.  From the blog article, it's much better to log in each stage and turn on debugging.

```scala
Source(infiniteTransactionsStream)
 .log(“got transaction”)
 .grouped(5)
 .log(“grouped transactions”)
 .scan(0.hashCode.toString) { (previousBlockHash, transactions) => 
   (transactions.hashCode(), previousBlockHash).hashCode.toString 
 }
 .log(“hashed block”)
 .mapAsync(1)(saveHashAndReturnIt)
 .log(“saved block”)
 .withAttributes(logLevels(onElement = DebugLevel))
 ```

The default operation in Akka is `FlowOps.log`, which takes a name and an element operation.  The `log` operator also requires an implicit `LoggingAdapter` -- and if none is found then it will compile fine, but not log!

As such, the easiest thing to do to ensure proper logging is to provide the correct context by default.  This means adding a `HasLoggingAdapter` trait with an `implicit val loggingAdapter`:

```scala
import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}

trait HasLoggingAdapter {
  implicit val loggingAdapter: LoggingAdapter

  def fromSystem(implicit system: ActorSystem): LoggingAdapter = Logging.getLogger(system, this)
}
```

And then from there, requiring the implementation of the trait to have a logging adapter from the actor system, and adding the appropriate field builder to convert the element to a value:

```scala
object Main {
  private implicit val actorSystem: ActorSystem = ActorSystem("example")

  def main(args: Array[String]): Unit = {
    DoThing().runWith(Sink.ignore)
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
```
