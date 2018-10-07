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
abstract class SettingsConfigurable[T] extends JComponent with Disposable with SettingsComponent {
  protected var settingsComponents: Seq[SettingsElement[T]] = Seq()

  override def dispose(): Unit = {
    settingsComponents = Seq()
  }

  def isModified: Boolean = settingsComponents.exists(_.isModified)

  def save(): Unit = settingsComponents.foreach(_.save())

  def reset(): Unit = settingsComponents.foreach(_.reset())

  protected def wrap(container: JComponent, component: JComponent): Unit = {
    val wrapper: JPanel = new JPanel()
    wrapper.setLayout(new FlowLayout(FlowLayout.LEFT))
    container.add(component)
  }
}
