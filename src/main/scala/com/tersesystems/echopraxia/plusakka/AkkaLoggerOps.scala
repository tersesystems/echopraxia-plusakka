package com.tersesystems.echopraxia.plusakka

import akka.actor.typed.{Behavior, BehaviorInterceptor, LogOptions, Signal, TypedActorContext}
import akka.actor.typed.scaladsl.Behaviors
import com.tersesystems.echopraxia.api.{CoreLogger, FieldBuilderResult, Utilities}
import com.tersesystems.echopraxia.plusscala.{DefaultLoggerMethods, Logger}
import com.tersesystems.echopraxia.plusscala.api.{AbstractLoggerSupport, Condition, FieldBuilder, LoggerSupport}

import scala.compat.java8.FunctionConverters._
import scala.reflect.ClassTag

object AkkaLoggerOps {

  implicit class AkkaLogger[FB <: AkkaFieldBuilder](logger: Logger[FB]) {

    private type ToValue[T] = logger.fieldBuilder.ToValue[T]

    // XXX Add this through type enrichment
    def logMessages[T: logger.fieldBuilder.ToValue : ClassTag](behavior: Behavior[T]): Behavior[T] =
      Behaviors.intercept(() => new LoggingInterceptor[T](LogOptions(), logger))(behavior)

    class LoggingInterceptor[T: ToValue : ClassTag](val opts: LogOptions, logger: Logger[FB]) extends BehaviorInterceptor[T, T] {
      import BehaviorInterceptor._
      import LoggingInterceptor._

      override def aroundReceive(ctx: TypedActorContext[T], msg: T, target: ReceiveTarget[T]): Behavior[T] = {
        logMessage(LogMessageTemplate, msg, ctx)
        target(ctx, msg)
      }

      override def aroundSignal(ctx: TypedActorContext[T], signal: Signal, target: SignalTarget[T]): Behavior[T] = {
        logSignal(LogSignalTemplate, signal, ctx)
        target(ctx, signal)
      }

      private def logSignal(template: String, signal: Signal, context: TypedActorContext[T]): Unit = {
        if (opts.enabled) {
          val selfPath = context.asScala.self.path
          val fbf: FB => FieldBuilderResult = fb => fb.list(
            fb.value("selfPath", fb.ToValue(selfPath)(fb.actorPathToValue)),
            fb.value("signal" -> fb.ToValue(signal)(fb.signalToValue))
          )
          log(template, fbf)
        }
      }

      private def logMessage(template: String, message: T, context: TypedActorContext[T]): Unit = {
        if (opts.enabled) {
          val selfPath = context.asScala.self.path
          val fbf: FB => FieldBuilderResult = fb => fb.list(
            fb.value("selfPath", fb.ToValue(selfPath)(fb.actorPathToValue)),
            fb.value("message" -> implicitly[ToValue[T]].toValue(message))
          )
          log(template, fbf)
        }
      }

      private def log(template: String, fbf: FB => FieldBuilderResult): Unit = {
        opts.level match {
          case org.slf4j.event.Level.ERROR => logger.error(template, fbf)
          case org.slf4j.event.Level.WARN => logger.warn(template, fbf)
          case org.slf4j.event.Level.INFO => logger.info(template, fbf)
          case org.slf4j.event.Level.DEBUG => logger.debug(template, fbf)
          case org.slf4j.event.Level.TRACE => logger.trace(template, fbf)
        }
      }

      // only once in the same behavior stack
      override def isSame(other: BehaviorInterceptor[Any, Any]): Boolean = other match {
        case a: LoggingInterceptor[_] => a.opts == opts
        case _ => false
      }
    }

    object LoggingInterceptor {
      private val LogMessageTemplate = "actor [{}] received message: {}"
      private val LogSignalTemplate = "actor [{}] received signal: {}"
    }

  }


}
