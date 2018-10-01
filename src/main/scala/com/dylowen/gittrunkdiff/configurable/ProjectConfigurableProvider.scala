package com.dylowen.gittrunkdiff.configurable

import java.awt.FlowLayout
import javax.swing._

import com.dylowen.gittrunkdiff.settings.ProjectSettings
import com.dylowen.gittrunkdiff.utils.Utils
import com.intellij.openapi.project.Project
import git4idea.GitBranch
import git4idea.repo.GitRepository

import scala.collection.JavaConversions._

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Aug-2016
  */
class ProjectConfigurableProvider(val project: Project) extends ConfigurableProviderImpl {
  implicit val implProject = project

  override def getComponent: SettingsConfigurable = new SettingsConfigurable {
    {
      val masterBranches: Array[MasterBranchBox] = Utils.getGitRepos(project)().map(new MasterBranchBox(_))
      this.settingsComponents = masterBranches.asInstanceOf[Array[SettingsElement[_]]]

      setLayout(new FlowLayout(FlowLayout.LEFT))
      val wrapper: JPanel = new JPanel()
      wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS))

      for (branchBox <- masterBranches) {
        wrap(wrapper, branchBox.getLabel)
        wrap(wrapper, branchBox)
      }

      add(wrapper)
    }

    reset()

    private class MasterBranchBox(val gitRepo: GitRepository) extends JComboBox[String] with SettingsElement[Int] {
      val branches: Array[String] = gitRepo.getBranches.getLocalBranches.map(_.getName).toArray
      setModel(new DefaultComboBoxModel[String](branches))

      def getLabel: JLabel = new JLabel(gitRepo.getRoot.getCanonicalPath)

      private def getBranch: GitBranch = {
        val branchName: String = getSelectedItem.asInstanceOf[String]

        Utils.getBranch(branchName, gitRepo).get
      }

      def getVisualValue: Int = getSelectedIndex

      def setVisualValue(value: Int): Unit = setSelectedIndex(value)

      def getSetting: Int = branches.indexOf(ProjectSettings.getMasterBranch(gitRepo).getName)

      def setSetting(value: Int): Unit = ProjectSettings.setMasterBranch(gitRepo, getBranch)

      init()
    }
  }
}