package com.dylowen.gittrunkdiff

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Condition
import com.intellij.openapi.wm.{ToolWindow, ToolWindowFactory}

//import git4idea.GitVcs

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Apr-2016
  */
class MainToolWindow extends ToolWindowFactory with Condition[Project] {
  def createToolWindowContent(project: Project, toolWindow: ToolWindow): Unit = {
    //val gitDiffView: GitDiffView = GitDiffView.getInstance(project)

    //gitDiffView.init(toolWindow)
  }

  /**
    * @return whether git is enabled for this project
    */
  def value(project: Project): Boolean = Utils.validForProject(project)
}
