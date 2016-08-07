package com.dylowen.gittrunkdiff.config

import java.awt.{BorderLayout, FlowLayout, GridLayout}
import javax.swing._

import com.dylowen.gittrunkdiff.Utils
import com.intellij.openapi.Disposable
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
class GitTrunkSettingsComponent(val project: Project) extends JPanel with Disposable {
  var masterBranches: Array[MasterBranchBox] = Utils.getGitRepos(project)().map(new MasterBranchBox(_, project))

  {
    setLayout(new FlowLayout(FlowLayout.LEFT))

    val wrapper: JPanel = new JPanel()
    wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS))

    for (branchBox <- masterBranches) {
      wrapper.add(branchBox.getLabel)
      wrapper.add(branchBox)
    }

    add(wrapper)
  }

  override def dispose(): Unit = this.masterBranches = Array()

  def isModified: Boolean = !this.masterBranches.forall(!_.isModified)

  def apply(): Unit = this.masterBranches.foreach(_.apply())

  def reset(): Unit = this.masterBranches.foreach(_.reset())
}

class MasterBranchBox(val gitRepo: GitRepository, implicit val project: Project) extends JComboBox[String] {
  var selectedIndex = -1

  {
    val branches: Array[String] = gitRepo.getBranches.getLocalBranches.map(_.getName).toArray
    val chosenBranch: GitBranch = Settings.getMasterBranch(gitRepo)
    selectedIndex = branches.indexOf(chosenBranch.getName)

    if (selectedIndex == -1) {
      selectedIndex = branches.indexOf(Utils.guessMasterBranch(gitRepo).getName)
    }

    setModel(new DefaultComboBoxModel[String](branches))
    setSelectedIndex(selectedIndex)
  }

  def getLabel: JLabel = new JLabel(gitRepo.getRoot.getCanonicalPath)

  private def getBranch: GitBranch = {
    val branchName: String = getSelectedItem.asInstanceOf[String]

    Utils.getBranch(branchName, gitRepo).get
  }

  def isModified: Boolean = this.selectedIndex != getSelectedIndex

  def apply(): Unit = {
    if (isModified) {
      this.selectedIndex = getSelectedIndex
      val branch: GitBranch = getBranch

      Settings.setMasterBranch(gitRepo, branch)
    }
  }

  def reset(): Unit = setSelectedIndex(this.selectedIndex)
}