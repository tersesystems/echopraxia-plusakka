package example

import com.tersesystems.echopraxia.plusakka.actor.AkkaFieldBuilderProvider
import com.tersesystems.echopraxia.plusakka.actor.typed.DefaultAkkaTypedFieldBuilder

trait HelloWorldFieldBuilder extends DefaultAkkaTypedFieldBuilder {
  implicit val greetToValue: ToObjectValue[Greet] = { greet: Greet =>
    ToObjectValue(
      keyValue("whom", greet.whom),
      keyValue("replyTo", greet.replyTo)
    )
  }
  implicit val greetedToValue: ToObjectValue[Greeted] = { greet: Greeted =>
    ToObjectValue(
      keyValue("whom", greet.whom),
      keyValue("replyTo", greet.from)
    )
  }
  implicit val sayHelloToValue: ToObjectValue[HelloWorldMain.SayHello] = { sayHello =>
    ToObjectValue(
      Seq(keyValue("name", sayHello.name)):_*
    )
  }
}

object HelloWorldFieldBuilder extends HelloWorldFieldBuilder

trait HelloWorldFieldBuilderProvider extends AkkaFieldBuilderProvider {
  override type FieldBuilderType = HelloWorldFieldBuilder.type
  protected def fieldBuilder: FieldBuilderType = valueOf[FieldBuilderType]
}
