package com.tersesystems.echopraxia.plusakka

import com.tersesystems.echopraxia.plusscala.generic.SemiAutoDerivation

trait HelloWorldFieldBuilder extends DefaultAkkaFieldBuilder with SemiAutoDerivation {
  implicit lazy val greetToValue: ToValue[HelloWorld.Greet] = gen[HelloWorld.Greet]
  implicit lazy val greetedToValue: ToValue[HelloWorld.Greeted] = gen[HelloWorld.Greeted]
  implicit lazy val sayHelloToValue: ToValue[HelloWorldMain.SayHello] = gen[HelloWorldMain.SayHello]
}

object HelloWorldFieldBuilder extends HelloWorldFieldBuilder

trait HelloWorldFieldBuilderProvider extends FieldBuilderProvider {
  override type FieldBuilderType = HelloWorldFieldBuilder.type
  protected def fieldBuilder: FieldBuilderType = valueOf[FieldBuilderType]
}
