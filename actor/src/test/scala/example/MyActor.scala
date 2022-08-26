package example

import akka.actor.Actor
import com.tersesystems.echopraxia.plusakka.actor.{ActorLogging, DefaultAkkaFieldBuilderProvider}

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