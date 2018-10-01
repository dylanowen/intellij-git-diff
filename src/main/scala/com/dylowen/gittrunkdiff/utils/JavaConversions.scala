package com.dylowen.gittrunkdiff.utils

import com.intellij.openapi.util.Condition
import com.intellij.util.Consumer

import scala.language.implicitConversions

/**
  * Some basic Java 8 Conversions
  *
  * @author dylan.owen
  * @since Oct-2016
  */
object JavaConversions {
  implicit def functionToRunnable(fun: () => Unit): Runnable = new Runnable {
    override def run(): Unit = fun()
  }

  implicit def functionToConsumer[T](consumer: (T) => Unit): Consumer[T] = new Consumer[T] {
    override def consume(value: T) = consumer(value)
  }

  implicit def functionToCondition[T](condition: () => Boolean): Condition[T] = new Condition[T] {
    override def value(t: T): Boolean = condition()
  }

  implicit def functionToCondition[T](condition: (T) => Boolean): Condition[T] = new Condition[T] {
    override def value(t: T): Boolean = condition(t)
  }
}
