package com.dylowen.gittrunkdiff

import com.dylowen.gittrunkdiff.settings.ApplicationSettings
import com.dylowen.gittrunkdiff.toolwindow.GitDiffToolContent
import com.dylowen.gittrunkdiff.utils.Utils
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.Condition
import com.intellij.openapi.wm.{ToolWindow, ToolWindowFactory}
import com.intellij.ui.content.ContentFactory

//import git4idea.GitVcs

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Apr-2016
  */
class MainToolWindow extends ToolWindowFactory with Condition[Project] {
  def createToolWindowContent(project: Project, toolWindow: ToolWindow): Unit = {
    implicit val iProject = project

    val gitDiffView: GitDiffToolContent = new GitDiffToolContent()

    val content = ContentFactory.SERVICE.getInstance().createContent(gitDiffView, "", false)
    content.setCloseable(true)

    toolWindow.getContentManager.addContent(content)
  }

  /**
    * @return whether git is enabled for this project
    */
  def value(project: Project): Boolean = Utils.validForProject(project) && ApplicationSettings.getShowOwnToolbar()
}
