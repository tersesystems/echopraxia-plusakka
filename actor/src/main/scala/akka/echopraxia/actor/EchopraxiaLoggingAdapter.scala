package akka.echopraxia.actor

import com.tersesystems.echopraxia.api.{CoreLogger, CoreLoggerFactory}
import com.tersesystems.echopraxia.plusscala.Logger


trait EchopraxiaLoggingAdapter[FB] {
  def core: CoreLogger

  def fieldBuilder: FB
}

object EchopraxiaLoggingAdapter {

  def apply[FB](logger: Logger[FB]): EchopraxiaLoggingAdapter[FB] = new EchopraxiaLoggingAdapter[FB] {
    override def core: CoreLogger = logger.core
    override def fieldBuilder: FB = logger.fieldBuilder
  }

  def apply[FB](name: String, fb: FB): EchopraxiaLoggingAdapter[FB] = new EchopraxiaLoggingAdapter[FB] {
    override val core: CoreLogger = CoreLoggerFactory.getLogger("", name)
    override val fieldBuilder: FB = fb
  }

  def apply[FB](clazz: Class[_], fb: FB): EchopraxiaLoggingAdapter[FB] = apply(clazz.getName, fb)
}
