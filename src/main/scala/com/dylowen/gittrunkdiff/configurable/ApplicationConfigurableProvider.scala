package com.dylowen.gittrunkdiff.configurable

import java.awt.FlowLayout

import com.dylowen.gittrunkdiff.settings.ApplicationSettings
import javax.swing.{BoxLayout, JCheckBox, JPanel}

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Aug-2016
  */
class ApplicationConfigurableProvider() extends ConfigurableProviderImpl {
  override def getComponent: SettingsConfigurable[Boolean] = new SettingsConfigurable[Boolean] {
    {
      val showOwnToolbar: ShowOwnToolbarCheckBox = new ShowOwnToolbarCheckBox("Show In Own Toolbar")
      this.settingsComponents = Seq(showOwnToolbar)

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

      def getSetting: Boolean = ApplicationSettings.showOwnToolbar

      def setSetting(value: Boolean): Unit = {
        ApplicationSettings.showOwnToolbar = value
      }

      init()
    }
  }
}