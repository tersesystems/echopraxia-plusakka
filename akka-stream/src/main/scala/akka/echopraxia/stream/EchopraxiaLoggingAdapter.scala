package akka.echopraxia.stream
import com.tersesystems.echopraxia.api.{CoreLogger, CoreLoggerFactory}


trait EchopraxiaLoggingAdapter[FB] {
  def core: CoreLogger

  def fieldBuilder: FB
}

object EchopraxiaLoggingAdapter {

  def apply[FB <: AkkaStreamFieldBuilder](name: String, fb: FB): EchopraxiaLoggingAdapter[FB] = new EchopraxiaLoggingAdapter[FB] {
    override val core: CoreLogger = CoreLoggerFactory.getLogger("", name)
    override val fieldBuilder: FB = fb
  }

  def apply[FB <: AkkaStreamFieldBuilder](clazz: Class[_], fb: FB): EchopraxiaLoggingAdapter[FB] = apply(clazz.getName, fb)
}
