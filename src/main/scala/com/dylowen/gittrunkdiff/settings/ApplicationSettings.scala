package com.dylowen.gittrunkdiff.settings

import com.intellij.openapi.project.Project

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Aug-2016
  */
object ApplicationSettings {
  private val showOwnToolbarKey: String = "showOwnToolbar"

  def getShowOwnToolbar(): Boolean = {
    val showOwnToolbar: Option[Boolean] = Settings.application.getBoolean(showOwnToolbarKey)
    if (showOwnToolbar.isDefined) {
      showOwnToolbar.get
    }
    else {
      setShowOwnToolbar(true)

      true
    }
  }

  def setShowOwnToolbar(showOwnToolbar: Boolean): Unit = {
    Settings.application.setBoolean(showOwnToolbarKey, showOwnToolbar)
  }
}
