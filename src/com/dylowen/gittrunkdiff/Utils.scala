package com.dylowen.gittrunkdiff

import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.project.Project
import com.intellij.util.NotNullFunction
import git4idea.GitVcs

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Apr-2016
  */
object Utils {
  /**
    * @return whether this plugin is valid for this project
    */
  def validForProject(project: Project): Boolean = !project.isDisposed && gitVcsIsActive(project)

  /**
    * @return whether git vcs is active for this project
    */
  private def gitVcsIsActive(project: Project): Boolean = {
    val gitVcs = GitVcs.getInstance(project)

    ProjectLevelVcsManager.getInstance(project).checkVcsIsActive(gitVcs)
  }
}

class ShowVcsTab extends NotNullFunction[Project, Boolean] {
  override def fun(project: Project): Boolean = Utils.validForProject(project)
}