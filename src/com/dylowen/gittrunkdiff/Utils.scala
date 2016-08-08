package com.dylowen.gittrunkdiff

import java.util

import com.dylowen.gittrunkdiff.settings.ProjectSettings
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.{ProjectLevelVcsManager, VcsRoot}
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.NotNullFunction
import git4idea.repo.{GitRepository, GitRepositoryImpl}
import git4idea.{GitBranch, GitLocalBranch, GitVcs}

import scala.collection.JavaConversions._

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Apr-2016
  */
object Utils {
  private val MASTER_NAMES: Array[String] = Array("svn/trunk", "master")

  /**
    * @return whether this plugin is valid for this project
    */
  def validForProject(implicit project: Project): Boolean = !project.isDisposed && gitVcsIsActive(project)

  type GitReposGetter = () => Array[GitRepository]

  def getGitRepos(implicit project: Project): GitReposGetter = {
    val gitVcs = GitVcs.getInstance(project)
    val projectVcsManager: ProjectLevelVcsManager = ProjectLevelVcsManager.getInstance(project)

    () => projectVcsManager.getAllVcsRoots.
      filter(_.getVcs.equals(gitVcs)).
      map((gitVcsRoot: VcsRoot) => {
        val repoRoot: VirtualFile = gitVcsRoot.getPath

        GitRepositoryImpl.getInstance(repoRoot, project, true)
      })
  }

  def guessMasterBranch(repo: GitRepository): GitBranch = {
    val branches: util.Collection[GitLocalBranch] = repo.getBranches.getLocalBranches

    var masterBranch: Option[GitBranch] = None
    var branchWeight: Int = Int.MaxValue

    //try to find the "master" branch
    for (branch: GitBranch <- branches if branchWeight > 0) {
      val index: Int = Utils.MASTER_NAMES.indexOf(branch.getName)

      if (index > -1) {
        masterBranch = Some(branch)
        branchWeight = index
      }
    }

    //fall back on any branch
    if (masterBranch.isEmpty) {
      masterBranch = Some(branches.iterator().next())
    }

    masterBranch.get
  }

  def getBranch(name: String, repo: GitRepository): Option[GitBranch] = Option(repo.getBranches.findLocalBranch(name))

  def getGitRepo(path: VirtualFile)(implicit project: Project): GitRepository = GitRepositoryImpl.getInstance(path, project, true)

  /**
    * @return whether git vcs is active for this project
    */
  private def gitVcsIsActive(project: Project): Boolean = {
    val gitVcs = GitVcs.getInstance(project)

    ProjectLevelVcsManager.getInstance(project).checkVcsIsActive(gitVcs)
  }
}