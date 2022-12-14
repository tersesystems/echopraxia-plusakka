package example

import akka.actor.Actor
import akka.echopraxia.actor.{ActorLogging, DefaultAkkaFieldBuilderProvider, EchopraxiaLoggingAdapter, LoggingReceive}

class MyActor extends Actor with ActorLogging with DefaultAkkaFieldBuilderProvider {

  override def preStart(): Unit = {
    log.debug("Starting")
  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    log.error("Restarting due to [{}] when processing [{}]", fb => fb.list(
      fb.exception(reason),
      fb.string("message" -> message.toString),
      fb.keyValue("self" -> self.path)
    ))
  }

  def receive = LoggingReceive {
    case "test" => log.info("Received test")
    case x      => log.warn("Received unknown message: {}", _.string("x" -> x.toString))
  }
}