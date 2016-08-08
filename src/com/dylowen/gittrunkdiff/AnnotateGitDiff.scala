package com.dylowen.gittrunkdiff

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.vcs.actions.AnnotateToggleAction

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Aug-2016
  */
class AnnotateGitDiff extends AnnotateToggleAction.Provider {
  override def isEnabled(e: AnActionEvent): Boolean = true

  override def isAnnotated(e: AnActionEvent): Boolean = false

  override def isSuspended(e: AnActionEvent): Boolean = false

  override def perform(e: AnActionEvent, selected: Boolean): Unit = {
    println("annotate")
  }
}
