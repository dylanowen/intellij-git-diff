package com.dylowen.gittrunkdiff.settings

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Aug-2016
  */
object Settings {
  val PROPERTIES_PREFIX: String = "com.dylowen.gitdiff."

  private def getBoolean(properties: PropertiesComponent): String => Option[Boolean] = (key: String) => {
    val fullKey: String = Settings.PROPERTIES_PREFIX + key

    if (properties.isValueSet(fullKey)) {
      Some(properties.getBoolean(fullKey))
    }
    else {
      None
    }
  }
  private def setBoolean(properties: PropertiesComponent) = (key: String, value: Boolean) => properties.setValue(Settings.PROPERTIES_PREFIX + key, value)
  private def getString(properties: PropertiesComponent) = (key: String) => Option(properties.getValue(Settings.PROPERTIES_PREFIX + key))
  private def setString(properties: PropertiesComponent) = (key: String, value: String) => properties.setValue(Settings.PROPERTIES_PREFIX + key, value)

  private def getProperties(properties: PropertiesComponent) = new Settings.Properties(properties)

  def project(implicit project: Project): Properties = {
    val properties: PropertiesComponent = PropertiesComponent.getInstance(project)

    getProperties(properties)
  }

  val application: Properties = {
    val properties: PropertiesComponent = PropertiesComponent.getInstance()

    getProperties(properties)
  }

  class Properties(properties: PropertiesComponent) {
    def getBoolean: String => Option[Boolean] = Settings.getBoolean(properties)
    def setBoolean = Settings.setBoolean(properties)
    def getString = Settings.getString(properties)
    def setString = Settings.setString(properties)
  }
}