package akka.echopraxia.actor

trait AkkaFieldBuilderProvider {
  type FieldBuilderType <: AkkaFieldBuilder
  protected def fieldBuilder: FieldBuilderType
}

trait DefaultAkkaFieldBuilderProvider extends AkkaFieldBuilderProvider {
  override type FieldBuilderType = DefaultAkkaFieldBuilder.type
  override protected def fieldBuilder: FieldBuilderType = DefaultAkkaFieldBuilder
}
