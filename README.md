# Echopraxia Plus Akka

This library provides Akka specific functionality for integrating [Echopraxia](https://github.com/tersesystems/echopraxia) structured logging into [Akka](https://akka.io).

This README assumes that you have `akka-slf4j` set up and have logging configured appropriately in Akka.

## Akka Actors

Adding Echopraxia Logging to Akka classic actors takes a little setup, but is hassle-free. 

There is an `AkkaFieldBuilder` trait that provides field builder mappings for common class `akka.actor.Address`, `akka.actor.ActorRef`, etc.

The `DefaultAkkaFieldBuilder` implementation fills out those mappings, and there's a `DefaultAkkaFieldBuilderProvider` that exposes that field builder.

```scala
trait DefaultAkkaFieldBuilderProvider extends AkkaFieldBuilderProvider {
  override type FieldBuilderType = DefaultAkkaFieldBuilder.type
  override protected def fieldBuilder: FieldBuilderType = DefaultAkkaFieldBuilder
}
```

The trait `ActorLogging` provides a logger, and wants a `AkkaFieldBuilderProvider` trait to be mixed in:

```scala
trait ActorLogging {
  this: Actor with AkkaFieldBuilderProvider =>
  protected val log: Logger[FieldBuilderType] = ...
}
```

This means that at the end of it, you can add `ActorLogging with DefaultAkkaFieldBuilderProvider` to your actor, and it will log appropriately.

```scala
import com.tersesystems.echopraxia.plusakka.actor._

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

Akka Typed logging uses the SLF4J logger API by default.  You can use the "Direct SLF4J API" from Echopraxia to pass through conditions and fields through markers, and arguments can be passed through the field builder interface as usual.

```scala
object HelloWorld extends HelloWorldFieldBuilder {
  def apply(): Behavior[Greet] = {
    Behaviors.receive { (context, message) =>
      // keyValue(name, ToValue) is provided by field builder API 
      context.log.info("Received message: {}", keyValue("foo", message))
      message.replyTo ! Greeted(message.whom, context.self)
      Behaviors.same
    }
  }
}
```

### Alternative to Behaviors.logMessages

The `com.tersesystems.echopraxia.plusakka.actor.typed.Implicits` trait contains `AkkaLoggerOps`, which has `logger.debugMessages`.  This can be used as an alternative to `Behaviors.logMessages` [logging](https://doc.akka.io/docs/akka/current/typed/logging.html#behaviors-logmessages).

```scala
import com.tersesystems.echopraxia.plusakka.actor.typed.Implicits._

val logger = LoggerFactory.getLogger.withFieldBuilder(HelloWorldFieldBuilder)

def apply(): Behavior[Greet] = logger.debugMessages[Greet] {
  Behaviors.receive { (context, message) =>
    message.replyTo ! Greeted(message.whom, context.self)
    Behaviors.same
  }
}
```

## Akka Streams

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