package com.dylowen.gittrunkdiff.settings

import com.dylowen.gittrunkdiff.Utils
import com.intellij.openapi.project.Project
import git4idea.GitBranch
import git4idea.repo.GitRepository
/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Aug-2016
  */
object ProjectSettings {
  private def masterBranchKey(repo: GitRepository): String = "masterBranch_" + repo.getRoot.getCanonicalPath

  def getMasterBranch(repo: GitRepository)(implicit project: Project): GitBranch = {
    val key: String = masterBranchKey(repo)
    val master: Option[GitBranch] = Settings.project(project).getString(key).flatMap(Utils.getBranch(_, repo))
    if (master.isDefined) {
      master.get
    }
    else {
      val masterBranch: GitBranch = Utils.guessMasterBranch(repo)
      setMasterBranch(repo, masterBranch)

      masterBranch
    }
  }

  def setMasterBranch(repo: GitRepository, branch: GitBranch)(implicit project: Project): Unit = {
    Settings.project(project).setString(masterBranchKey(repo), branch.getName)
  }
}
