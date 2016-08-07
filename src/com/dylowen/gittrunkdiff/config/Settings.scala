package com.dylowen.gittrunkdiff.config

import com.dylowen.gittrunkdiff.Utils
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import git4idea.GitBranch
import git4idea.repo.GitRepository
/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Aug-2016
  */
object Settings {
  private val PROPERTIES_PREFIX: String = "com.dylowen.gitdiff."

  private def masterBranchKey(repo: GitRepository): String = "masterBranch_" + repo.getRoot.getCanonicalPath


  def getMasterBranch(repo: GitRepository)(implicit project: Project): GitBranch = {
    val key: String = masterBranchKey(repo)
    val master: Option[GitBranch] = getString(key).flatMap(Utils.getBranch(_, repo))
    if (master.isDefined) {
      master.get
    }
    else {
      val masterBranch: GitBranch = Utils.guessMasterBranch(repo)
      setMasterBranch(repo, masterBranch)

      masterBranch
    }
  }

  def setMasterBranch(repo: GitRepository, branch: GitBranch)(implicit project: Project): Unit = setString(masterBranchKey(repo), branch.getName)

  private def getSettings(implicit project: Project): PropertiesComponent = PropertiesComponent.getInstance(project)
  private def getString(key: String)(implicit project: Project): Option[String] = Option(getSettings.getValue(Settings.PROPERTIES_PREFIX + key))
  private def setString(key: String, value: String)(implicit project: Project): Unit = getSettings.setValue(Settings.PROPERTIES_PREFIX + key, value)
}
