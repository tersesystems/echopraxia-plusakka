package akka.echopraxia.stream

import akka.stream.Materializer
import com.tersesystems.echopraxia.plusscala.{Logger, LoggerFactory}

trait EchopraxiaLoggingAdapter[FB] {
  def getLogger()(implicit mat: Materializer): Logger[FB]

  val fieldBuilder: FB
}

object EchopraxiaLoggingAdapter {

  def fromLogger[FB <: AkkaStreamFieldBuilder](logger: Logger[FB]): EchopraxiaLoggingAdapter[FB] = new EchopraxiaLoggingAdapter[FB] {
    override def getLogger()(implicit mat: Materializer): Logger[FB] = {
      val frozenMatField = fieldBuilder.keyValue("materializer" -> mat.supervisor.path)(fieldBuilder.actorPathToValue)
      logger.withFields(_ => frozenMatField)
    }

    override val fieldBuilder: FB = logger.fieldBuilder
  }

  def fromFieldBuilder[FB <: AkkaStreamFieldBuilder](fb: FB, name: String): EchopraxiaLoggingAdapter[FB] = new EchopraxiaLoggingAdapter[FB] {
    override def getLogger()(implicit mat: Materializer): Logger[FB] = {
      val frozenMatField = fieldBuilder.keyValue("materializer" -> mat.supervisor.path)(fieldBuilder.actorPathToValue)
      LoggerFactory.getLogger(name)
        .withFieldBuilder(fieldBuilder)
        .withFields(_ => frozenMatField)
    }

    override val fieldBuilder: FB = fb
  }
}
