package com.dylowen.gittrunkdiff.settings

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Aug-2016
  */
object ApplicationSettings {
  private val ShowOwnToolbarKey: String = "showOwnToolbar"

  def showOwnToolbar: Boolean = {
    Settings.application.getBoolean(ShowOwnToolbarKey)
      .getOrElse({
        showOwnToolbar = true

        true
      })
  }

  def showOwnToolbar_=(showOwnToolbar: Boolean): Unit = {
    Settings.application.setBoolean(ShowOwnToolbarKey, showOwnToolbar)
  }
}
