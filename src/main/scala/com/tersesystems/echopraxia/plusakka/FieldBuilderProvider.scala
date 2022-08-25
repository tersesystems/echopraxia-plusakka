package com.tersesystems.echopraxia.plusakka


trait FieldBuilderProvider {
  type FieldBuilderType <: AkkaFieldBuilder with Singleton
  protected def fieldBuilder: FieldBuilderType
}

trait DefaultFieldBuilderProvider extends FieldBuilderProvider {
  override type FieldBuilderType = DefaultAkkaFieldBuilder.type
  override protected def fieldBuilder: FieldBuilderType = valueOf[FieldBuilderType]
}
