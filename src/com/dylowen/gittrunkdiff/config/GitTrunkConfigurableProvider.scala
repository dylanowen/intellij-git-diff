package com.dylowen.gittrunkdiff.config

import javax.swing._

import com.dylowen.gittrunkdiff.Utils
import com.intellij.openapi.options.{Configurable, ConfigurableProvider}
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Aug-2016
  */
class GitTrunkConfigurableProvider(val project: Project) extends ConfigurableProvider {
  override def canCreateConfigurable: Boolean = Utils.validForProject(project)

  override def createConfigurable(): Configurable = new GitTrunkConfigurable(project)
}

class GitTrunkConfigurable(val project: Project) extends Configurable {

  var component: Option[GitTrunkSettingsComponent] = None

  override def getDisplayName: String = "Git Trunk"

  override def getHelpTopic: String = null

  override def isModified: Boolean = this.component.exists(_.isModified)

  override def createComponent(): JComponent = {
    if (this.component.isEmpty) {
      this.component = Some(new GitTrunkSettingsComponent(this.project))
    }

    this.component.get
  }

  override def disposeUIResources(): Unit = {
    this.component.foreach(Disposer.dispose(_))
    this.component = None
  }

  override def apply(): Unit = {
    this.component.foreach(_.apply())


  }

  override def reset(): Unit = this.component.foreach(_.reset())
}