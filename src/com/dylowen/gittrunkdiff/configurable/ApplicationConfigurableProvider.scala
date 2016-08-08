package com.dylowen.gittrunkdiff.configurable

import java.awt.FlowLayout
import javax.swing.{BoxLayout, JCheckBox, JPanel}

import com.dylowen.gittrunkdiff.settings.ApplicationSettings
import com.intellij.openapi.project.Project

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Aug-2016
  */
class ApplicationConfigurableProvider() extends ConfigurableProviderImpl {
  override def getComponent: SettingsConfigurable = new SettingsConfigurable {
    {
      val showOwnToolbar: ShowOwnToolbarCheckBox = new ShowOwnToolbarCheckBox("Show In Own Toolbar")

      setLayout(new FlowLayout(FlowLayout.LEFT))
      val wrapper: JPanel = new JPanel()
      wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS))

      wrap(wrapper, showOwnToolbar)

      add(wrapper)
    }

    reset()

    private class ShowOwnToolbarCheckBox(label: String) extends JCheckBox(label) with SettingsElement[Boolean] {
      def getVisualValue: Boolean = isSelected

      def setVisualValue(value: Boolean): Unit = setSelected(value)

      def getSetting: Boolean = ApplicationSettings.getShowOwnToolbar

      def setSetting(value: Boolean): Unit = ApplicationSettings.setShowOwnToolbar(value)

      init()
    }

  }
}