package com.dylowen.gittrunkdiff.configurable

import java.awt.FlowLayout
import javax.swing.{JComponent, JPanel}

import com.intellij.openapi.Disposable

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Aug-2016
  */
abstract class SettingsConfigurable extends JComponent with Disposable with SettingsComponent {
  protected var settingsComponents: Array[SettingsElement[_]] = Array()

  override def dispose(): Unit = this.settingsComponents = Array()

  def isModified: Boolean = !this.settingsComponents.forall(!_.isModified)

  def apply(): Unit = this.settingsComponents.foreach(_.apply())

  def reset(): Unit = this.settingsComponents.foreach(_.reset())

  protected def wrap(container: JComponent, component: JComponent): Unit = {
    val wrapper: JPanel = new JPanel()
    wrapper.setLayout(new FlowLayout(FlowLayout.LEFT))
    container.add(component)
  }

}
