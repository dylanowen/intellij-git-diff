package com.dylowen.gittrunkdiff.configurable

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Aug-2016
  */
trait SettingsElement[T] extends SettingsComponent {
  var lastValue: T = _

  def getVisualValue: T

  def setVisualValue(value: T): Unit

  def getSetting: T

  def setSetting(value: T): Unit

  override def isModified: Boolean = !this.lastValue.equals(getVisualValue)

  override def save(): Unit = {
    val value: T = getVisualValue
    setSetting(value)
    this.lastValue = value
  }

  override def reset(): Unit = setVisualValue(this.lastValue)

  protected def init(): Unit = {
    lastValue = getSetting
    reset()
  }
}