package com.dylowen.gittrunkdiff

import com.dylowen.gittrunkdiff.settings.ApplicationSettings
import com.dylowen.gittrunkdiff.utils.Utils
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Condition
import com.intellij.openapi.wm.{ToolWindow, ToolWindowFactory}
import com.intellij.ui.content.{Content, ContentFactory}

//import git4idea.GitVcs

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Apr-2016
  */
class MainToolWindow extends ToolWindowFactory with Condition[Project] {
  override def createToolWindowContent(project: Project, toolWindow: ToolWindow): Unit = {
    implicit val _project: Project = project

    val gitDiffManager: GitDiffManager = new GitDiffManager()

    val content: Content = ContentFactory.SERVICE.getInstance().createContent(gitDiffManager.panel, "", false)
    content.setCloseable(true)

    toolWindow.getContentManager.addContent(content)
  }

  /**
    * @return whether git is enabled for this project
    */
  override def value(project: Project): Boolean = Utils.validForProject(project) && ApplicationSettings.showOwnToolbar

}
