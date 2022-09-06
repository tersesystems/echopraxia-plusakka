package akka.echopraxia.stream

import akka.echopraxia.actor.{AkkaFieldBuilder, DefaultAkkaFieldBuilder}
import akka.stream._
import akka.stream.impl.TraversalBuilder
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.stage.GraphStage

trait AkkaStreamFieldBuilder extends AkkaFieldBuilder {

  implicit def flowToValue[In, Out, Mat]: ToValue[Flow[In, Out, Mat]]

  implicit def sourceToValue[Out, Mat]: ToValue[Source[Out, Mat]]

  implicit def sinkToValue[In, Mat]: ToValue[Sink[In, Mat]]

  implicit def sinkShapeToValue[T]: ToValue[SinkShape[T]]

  implicit def sourceShapeToValue: ToValue[SourceShape[_]]

  implicit def flowShapeToValue: ToValue[FlowShape[_, _]]

  implicit def graphToValue: ToValue[GraphStage[_]]

  implicit def inletToValue: ToValue[Inlet[_]]

  implicit def outletToValue: ToValue[Outlet[_]]

  implicit def traversalBuilderToValue: ToValue[TraversalBuilder]

  implicit def attributesToValue: ToValue[Attributes]

  implicit def attributeToValue: ToValue[Attributes.Attribute]

  implicit def shapeToValue: ToValue[Shape]

  implicit def restartSettingsToValue: ToValue[RestartSettings]

  implicit def queueOfferResultToValue: ToValue[QueueOfferResult]
}

class DefaultAkkaStreamFieldBuilder extends AkkaStreamFieldBuilder with DefaultAkkaFieldBuilder {
  override implicit def flowToValue[In, Out, Mat]: ToValue[Flow[In, Out, Mat]] = flow =>
    ToObjectValue(
      keyValue("shape" -> flow.shape),
      keyValue("traversalBuilder" -> flow.traversalBuilder)
    )

  override implicit def sourceToValue[Out, Mat]: ToValue[Source[Out, Mat]] = source =>
    ToObjectValue(
      keyValue("shape" -> source.shape),
      keyValue("traversalBuilder" -> source.traversalBuilder),
    )

  override implicit def sinkToValue[In, Mat]: ToValue[Sink[In, Mat]] = sink =>
    ToObjectValue(
      keyValue("shape" -> sink.shape),
      keyValue("traversalBuilder" -> sink.traversalBuilder)
    )

  override implicit def sinkShapeToValue[T]: ToValue[SinkShape[T]] = sinkShape =>
    ToValue(
      keyValue("in" -> sinkShape.in)
    )

  override implicit def sourceShapeToValue: ToValue[SourceShape[_]] = sourceShape =>
    ToValue(
      keyValue("out" -> sourceShape.out)
    )

  override implicit def flowShapeToValue: ToValue[FlowShape[_, _]] = flowShape =>
    ToObjectValue(
      keyValue("in" -> flowShape.in),
      keyValue("out" -> flowShape.out)
    )

  override implicit def inletToValue: ToValue[Inlet[_]] = inlet =>
    ToValue(inlet.toString)

  override implicit def outletToValue: ToValue[Outlet[_]] = outlet =>
    ToValue(outlet.toString)

  override implicit def traversalBuilderToValue: ToValue[TraversalBuilder] = { tb =>
      ToObjectValue(
        keyValue("attributes", tb.attributes),
        keyValue("isTraversalComplete", tb.isTraversalComplete),
      )
  }

  override implicit def attributesToValue: ToValue[Attributes] = { attrs =>
    ToArrayValue(attrs.attributeList)
  }

  override implicit def attributeToValue: ToValue[Attributes.Attribute] = { attr =>
    ToValue(attr.toString)
  }

  override implicit def shapeToValue: ToValue[Shape] = { shape =>
    ToObjectValue(
      keyValue("inlets" -> shape.inlets),
      keyValue("outlets" -> shape.outlets)
    )
  }

  override implicit def restartSettingsToValue: ToValue[RestartSettings] = { rs =>
    ToObjectValue(
      keyValue("minBackoff" -> rs.minBackoff.toString),
      keyValue("maxBackoff" -> rs.maxBackoff.toString),
      keyValue("maxRestarts" -> rs.maxRestarts),
      keyValue("maxRestartsWithin" -> rs.maxRestartsWithin.toString()),
      keyValue("randomFactor" -> rs.randomFactor),
    )
  }

  override implicit def queueOfferResultToValue: ToValue[QueueOfferResult] = { qor =>
    ToValue(qor.toString)
  }

  override implicit def graphToValue: ToValue[GraphStage[_]] = { graphStage =>
    ToObjectValue(
      keyValue("" -> graphStage.at)
    )
  }
}

object DefaultAkkaStreamFieldBuilder extends DefaultAkkaStreamFieldBuilder