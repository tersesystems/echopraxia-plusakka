package com.tersesystems.echopraxia.plusakka

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed._
import com.tersesystems.echopraxia.api.{FieldBuilderResult, Level}
import com.tersesystems.echopraxia.plusscala.Logger

import scala.reflect.ClassTag

object Implicits {

  implicit class AkkaLoggerOps[FB <: AkkaFieldBuilder](logger: Logger[FB]) {

    private type ToValue[T] = logger.fieldBuilder.ToValue[T]

    def logMessages[T: ToValue : ClassTag](level: Level)(behavior: Behavior[T]): Behavior[T] =
      Behaviors.intercept(() => new LogMessagesInterceptor[T](level, logger))(behavior)

    def logMessages[T: ToValue : ClassTag](behavior: Behavior[T]): Behavior[T] =
      Behaviors.intercept(() => new LogMessagesInterceptor[T](Level.DEBUG, logger))(behavior)

    class LogMessagesInterceptor[T: ToValue : ClassTag](val level: Level, logger: Logger[FB]) extends BehaviorInterceptor[T, T] {
      import BehaviorInterceptor._
      import LogMessagesInterceptor._

      override def aroundReceive(ctx: TypedActorContext[T], msg: T, target: ReceiveTarget[T]): Behavior[T] = {
        log(LogMessageTemplate, fb => {
          // very odd implicit behavior here, we need to manage ToValue explicitly
          fb.list(
            fb.value("self", ctx.asScala.self)(fb.typedActorRefToValue),
            fb.value("message" -> implicitly[ToValue[T]].toValue(msg))
          )
        })
        target(ctx, msg)
      }

      override def aroundSignal(ctx: TypedActorContext[T], signal: Signal, target: SignalTarget[T]): Behavior[T] = {
        log(LogSignalTemplate, fb => {
          import fb._
          fb.list(
            fb.value("self", ctx.asScala.self),
            fb.value("signal" -> signal)
          )
        })
        target(ctx, signal)
      }

      private def log(template: String, fbf: FB => FieldBuilderResult): Unit = {
        level match {
          case Level.ERROR => logger.error(template, fbf)
          case Level.WARN => logger.warn(template, fbf)
          case Level.INFO => logger.info(template, fbf)
          case Level.DEBUG => logger.debug(template, fbf)
          case Level.TRACE => logger.trace(template, fbf)
        }
      }

      // only once in the same behavior stack
      override def isSame(other: BehaviorInterceptor[Any, Any]): Boolean = other match {
        case a: LogMessagesInterceptor[_] => a.level == level
        case _ => false
      }
    }

    object LogMessagesInterceptor {
      private val LogMessageTemplate = "actor [{}] received message: {}"
      private val LogSignalTemplate = "actor [{}] received signal: {}"
    }
  }
}
