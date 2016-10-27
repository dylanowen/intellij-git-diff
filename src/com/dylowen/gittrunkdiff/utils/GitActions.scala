package com.dylowen.gittrunkdiff.utils

import java.util

import com.dylowen.gittrunkdiff.settings.ProjectSettings
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.{Change, ChangeList}
import com.intellij.openapi.vcs.history.VcsRevisionNumber
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.vcsUtil.VcsFileUtil
import git4idea.changes.GitChangeUtils
import git4idea.commands.{GitCommand, GitSimpleHandler}
import git4idea.repo.GitRepository
import git4idea.util.GitFileUtils
import git4idea.{GitBranch, GitRevisionNumber}

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Aug-2016
  */
object GitActions {
  def mergeBase(branchA: GitBranch, branchB: GitBranch, gitRepository: GitRepository)(implicit project: Project): GitRevisionNumber = mergeBase(branchA, branchB, gitRepository.getRoot)

  def mergeBase(branchA: GitBranch, branchB: GitBranch, root: VirtualFile)(implicit project: Project): GitRevisionNumber = mergeBase(branchA.getFullName, branchB.getFullName, root)

  def mergeBase(branchA: String, branchB: String, root: VirtualFile)(implicit project: Project): GitRevisionNumber = {
    val handler: GitSimpleHandler = new GitSimpleHandler(project, root, GitCommand.MERGE_BASE)

    handler.addParameters(branchA, branchB)
    handler.setSilent(true)
    handler.setStdoutSuppressed(true)

    val revisionString: String = handler.run().trim

    GitRevisionNumber.resolve(project, root, revisionString)
  }

  def getRevisionWhenBranched(gitRepo: GitRepository)(implicit project: Project): GitRevisionNumber = {
    val currentBranch: GitBranch = gitRepo.getCurrentBranch
    val master: GitBranch = ProjectSettings.getMasterBranch(gitRepo)

    GitActions.mergeBase(master, currentBranch, gitRepo)
  }

  def getFileAtRevision(file: VirtualFile, revisionNumber: VcsRevisionNumber, gitRepo: GitRepository)(implicit project: Project): String = {
    new String(getFileAtRevisionRaw(file, revisionNumber, gitRepo), file.getCharset)
  }

  def getFileAtRevisionRaw(file: VirtualFile, revisionNumber: VcsRevisionNumber, gitRepo: GitRepository)(implicit project: Project): Array[Byte] = {
    // get our repo root and the relative path for the file
    val repoRoot: VirtualFile = gitRepo.getRoot
    val relativePath: String = VcsFileUtil.relativePath(repoRoot, file)

    // get the bytes of the file at that revision
    GitFileUtils.getFileContent(project, repoRoot, revisionNumber.asString(), relativePath)
  }

  def getChangesSinceBranch(gitRepo: GitRepository)(implicit project: Project): Option[ChangeList] = {
    val currentBranch: GitBranch = gitRepo.getCurrentBranch
    val master: GitBranch = ProjectSettings.getMasterBranch(gitRepo)

    if (!master.equals(currentBranch)) {
      val repoRoot: VirtualFile = gitRepo.getRoot

      val lastBranchRevision: GitRevisionNumber = GitActions.mergeBase(master, currentBranch, repoRoot)

      val changes = GitChangeUtils.getDiff(project, repoRoot, lastBranchRevision.getRev, currentBranch.getName, null)

      Some(new GitBranchChangeList(currentBranch, changes))
    }
    else {
      // we don't care if we're on the master branch already
      None
    }
  }

  private class GitBranchChangeList(val branch: GitBranch, val changes: util.Collection[Change]) extends ChangeList {
    override def getName: String = this.branch.getName

    override def getComment: String = this.branch.getFullName

    override def getChanges: util.Collection[Change] = this.changes
  }
}
