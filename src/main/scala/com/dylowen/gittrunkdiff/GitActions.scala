package com.dylowen.gittrunkdiff

import java.util

import com.dylowen.gittrunkdiff.settings.ProjectSettings
import com.dylowen.gittrunkdiff.utils.Logging
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.changes.{Change, ChangeList}
import com.intellij.openapi.vcs.history.VcsRevisionNumber
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.vcsUtil.VcsFileUtil
import git4idea.changes.GitChangeUtils
import git4idea.history.GitHistoryUtils
import git4idea.repo.GitRepository
import git4idea.util.GitFileUtils
import git4idea.{GitBranch, GitRevisionNumber}

import scala.util.control.NonFatal

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Aug-2016
  */
object GitActions extends Logging {

  sealed trait GitError {
    def log(logger: Logger): Unit
  }

  case class GitException(exception: VcsException) extends GitError {
    override def log(logger: Logger): Unit = {
      logger.error(exception.getMessage)
    }
  }

  case class UnexpectedException(throwable: Throwable) extends GitError {
    override def log(logger: Logger): Unit = {
      logger.error(throwable.getMessage)
    }
  }


  def mergeBase(branchA: GitBranch, branchB: GitBranch, gitRepository: GitRepository)
               (implicit project: Project): Either[GitError, Option[GitRevisionNumber]] = {
    mergeBase(branchA, branchB, gitRepository.getRoot)
  }

  def mergeBase(branchA: GitBranch, branchB: GitBranch, root: VirtualFile)
               (implicit project: Project): Either[GitError, Option[GitRevisionNumber]] = {
    mergeBase(branchA.getFullName, branchB.getFullName, root)
  }

  def mergeBase(branchA: String, branchB: String, root: VirtualFile)
               (implicit project: Project): Either[GitError, Option[GitRevisionNumber]] = {
    try {
      Right(Option(GitHistoryUtils.getMergeBase(project, root, branchA, branchB)))
    }
    catch {
      case vcsException: VcsException => Left(GitException(vcsException))
      case NonFatal(t) => Left(UnexpectedException(t))
    }
  }

  def getRevisionWhenBranched(gitRepo: GitRepository)(implicit project: Project): GitRevisionNumber = {
    val currentBranch: GitBranch = gitRepo.getCurrentBranch
    val master: GitBranch = ProjectSettings.getMasterBranch(gitRepo)

    val repoRoot: VirtualFile = gitRepo.getRoot

    null /*
    mergeBase(master, currentBranch, gitRepo) match {
      case Right(maybeRevisionNumber) => maybeRevisionNumber
          .getOrElseThrow?
      case Left(gitError) => {
        gitError.log(logger)

        None
      }
    }
    */
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

      mergeBase(master, currentBranch, repoRoot) match {
        case Right(maybeRevisionNumber) => maybeRevisionNumber
          .map((lastBranchRevision: GitRevisionNumber) => {
            val changes: util.Collection[Change] = GitChangeUtils.getDiff(project, repoRoot, lastBranchRevision.getRev, currentBranch.getName, null)

            new GitBranchChangeList(currentBranch, changes)
          })
        case Left(gitError) => {
          gitError.log(logger)

          None
        }
      }
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
