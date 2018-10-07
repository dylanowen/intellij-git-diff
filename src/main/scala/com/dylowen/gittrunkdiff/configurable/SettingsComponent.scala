package com.dylowen.gittrunkdiff.configurable

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Aug-2016
  */
trait SettingsComponent {
  def isModified: Boolean

  def save(): Unit

  def reset(): Unit
}
