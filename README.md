# Echopraxia Plus Akka

This library provides Akka specific functionality for integrating [Echopraxia](https://github.com/tersesystems/echopraxia) structured logging into [Akka](https://akka.io)'s Scala API.  It provides pre-built default mappings for common Akka components such as paths, actors, and so on, and extends Akka Actors and Akka Streams with type enrichment for logging.  

This README assumes that you have `akka-slf4j` set up and have logging configured appropriately in Akka, and have the [Scala API](https://github.com/tersesystems/echopraxia-plusscala) in scope.

## Akka Actors

Akka Actor support can be imported using

```scala
libraryDependencies += "com.tersesystems.echopraxia.plusakka" %% "akka-actor" % plusAkkaVersion
```

### Field Builders

There is an `AkkaFieldBuilder` trait that provides field builder mappings for common class `akka.actor.Address`, `akka.actor.ActorRef`, etc.

The `DefaultAkkaFieldBuilder` implementation fills out those mappings, and there's a `DefaultAkkaFieldBuilderProvider` that exposes that field builder.

```scala
package akka.echopraxia.actor

trait DefaultAkkaFieldBuilderProvider extends AkkaFieldBuilderProvider {
  override type FieldBuilderType = DefaultAkkaFieldBuilder.type
  override protected def fieldBuilder: FieldBuilderType = DefaultAkkaFieldBuilder
}
```

### Actor Logging

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

Echopraxia support for Akka Typed can be imported

```scala
libraryDependencies += "com.tersesystems.echopraxia.plusakka" %% "akka-actor-typed" % plusAkkaVersion
```

### Field Builders

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

### Behaviors

Akka Typed incorporates the context through behaviors, using a context.  This context is added to `context.log` as MDC, but you can add it directly as context to the logger using `withActorContext`, which is a type enrichment method you can add with `import akka.echopraxia.actor.typed.Implicits._`:

```scala
package example

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import akka.echopraxia.actor.typed.DefaultAkkaTypedFieldBuilder
import akka.echopraxia.actor.typed.Implicits._
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
        .withActorContext(context)

      Behaviors.receiveMessage { echo =>
        logger.info("echoActor: {}", _.keyValue("echo", echo))
        Behaviors.same
      }
    }
  }
}
```

Type enrichment also adds `logger.debugMessages`, which can be used in a similar fashion to `Behaviors.logMessages` [logging](https://doc.akka.io/docs/akka/current/typed/logging.html#behaviors-logmessages).

```scala
object GreeterMain {
  import akka.echopraxia.actor.typed.Implicits._

  final case class SayHello(name: String)

  object MyFieldBuilder extends DefaultAkkaTypedFieldBuilder {
    implicit val sayHelloToValue: ToObjectValue[SayHello] = hello =>
      ToObjectValue(
        keyValue("@type" -> "SayHello"),
        keyValue("name" -> hello.name)
      )
    // ...
  }

  def apply(): Behavior[SayHello] =
    Behaviors.setup { context =>
      val greeter = context.spawn(Greeter(), "greeter")
      // Add ActorContext as field from setup
      val logger = LoggerFactory.getLogger.withFieldBuilder(MyFieldBuilder).withActorContext(context)
      
      // Then log SayHello messages
      logger.debugMessages[SayHello] {
        Behaviors.receiveMessage { message =>
          val replyTo = context.spawn(GreeterBot(max = 3), message.name)
          greeter ! Greeter.Greet(message.name, replyTo)
          Behaviors.same
        }
      }
    }
}
```

If you would rather use `context.log`, you can use the "Direct SLF4J API" from Echopraxia to pass through conditions and fields through markers, and arguments can be passed through the field builder interface as usual.

```scala
object HelloWorld extends MyFieldBuilder {
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

## Akka Streams

Echopraxia Akka Streams support can be installed using the following path:

```scala
libraryDependencies += "com.tersesystems.echopraxia.plusakka" %% "akka-stream" % plusAkkaVersion
```

### Field Builders

Akka Streams comes with a number of components, which can be tedious to map out.  The `DefaultAkkaStreamFieldBuilder` singleton object comes with support for `Source`, `Sink`, `Flow`, `Inlet`, `Outlet` and many more Akka Streams classes, 

```scala
import akka.echopraxia.stream.DefaultAkkaStreamFieldBuilder

trait MyFieldBuilder extends DefaultAkkaStreamFieldBuilder {
  // Add your own mappings
}
object MyFieldBuilder extends MyFieldBuilder

val logger = LoggerFactory.getLogger.withFieldBuilder(MyFieldBuilder)
```

### Logging Stage Support

There are two main reasons why Akka Streams should always incorporate logging of some sort in a source or flow.

The first reason is that by default, Akka Streams will [swallow exceptions](https://blog.softwaremill.com/akka-streams-pitfalls-to-avoid-part-1-75ef6403c6e6) and not log them.  As such, you need to have a `log` or `recover` op to make the exception visible.

The second reason is that logging is the [best way](https://blog.softwaremill.com/akka-streams-pitfalls-to-avoid-part-2-f93e60746c58) to see what's happening in a stream.  From the blog article, it's much better to log in each stage and turn on debugging.

You can't use a `LoggingAdapter` here at all, because `LoggingAdapter` throws away the original arguments and only logs a string template -- so logstash structuredArguments won't translate well, and you can't get JSON into the underlying logger.

Instead, you will need an `EchopraxiaLoggingAdapter` in implicit scope, with `Implicits._` providing the [type enrichment](https://doc.akka.io/docs/akka/current/stream/stream-customize.html#extending-flow-operators-with-custom-operators) needed for the logging operations:

```scala
import akka.echopraxia.stream.Implicits._
import akka.echopraxia.stream.EchopraxiaLoggingAdapter

implicit val loggingAdapter = EchopraxiaLoggingAdapter(this.getClass, MyFieldBuilder)
```

To log with structured logging, you'll call `elog`, add conditions and fields as appropriate, and then log at the appropriate level:

```scala
val s = Source(1 to 4)
  .elog
  .info("before", (fb, el) => fb.keyValue("elem", el))
```

Note that the `info` call takes a name indicating the stage, and also takes a function with the field builder and element, returning a `Field`.  You can elide this with `_.keyValue("elem", _)`, i.e.

```scala
val s = Source(1 to 4)
  .elog
  .debug("before", _.keyValue("elem", _))
```

You can add context fields and conditions as appropriate to the stage in a fluent manner:

```scala
val s = Source(1 to 4)
  .elog
  .withCondition(condition)
  .withFields(fb => fb.keyValue("foo", "bar"))
  .info("before", (fb, el) => fb.keyValue("elem", el))
 ```

Logging in a flow uses the same API.

```scala 
val f: Int => String = _.toString
Flow.fromFunction(f).log2.debug("name", _.keyValue("elem", _))
```

Logging in Akka Streams using the Java API is more involved, because type enrichment is not an option.  If you want to use structured logging in the Java API with Echopraxia, you will need to use 
[logWithMarker](https://doc.akka.io/japi/akka/2.6/akka/stream/javadsl/Source.html#logWithMarker(java.lang.String,akka.japi.function.Function)) with `FieldMarker` and `ConditionMarker` directly to bridge the gap.