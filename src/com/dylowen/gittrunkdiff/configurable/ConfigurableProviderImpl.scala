package com.dylowen.gittrunkdiff.configurable

import javax.swing.JComponent

import com.intellij.openapi.options.{Configurable, ConfigurableProvider}
import com.intellij.openapi.util.Disposer

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Aug-2016
  */
abstract class ConfigurableProviderImpl extends ConfigurableProvider {
  def getComponent: SettingsConfigurable

  def createConfigurable(): Configurable = new Configurable {
    var component: Option[SettingsConfigurable] = None

    override def getDisplayName: String = "Git Trunk"

    override def getHelpTopic: String = null

    override def createComponent(): JComponent = {
      if (this.component.isEmpty) {
        this.component = Some(getComponent)
      }

      this.component.get
    }

    override def disposeUIResources(): Unit = {
      this.component.foreach(Disposer.dispose(_))
      this.component = None
    }

    override def isModified: Boolean = this.component.exists(_.isModified)

    override def apply(): Unit = this.component.foreach(_.apply())

    override def reset(): Unit = this.component.foreach(_.reset())
  }
}


