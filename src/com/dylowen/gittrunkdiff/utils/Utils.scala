package com.dylowen.gittrunkdiff.utils

import java.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.{ProjectLevelVcsManager, VcsRoot}
import com.intellij.openapi.vfs.VirtualFile
import git4idea._
import git4idea.repo.{GitRepository, GitRepositoryImpl}

import scala.collection.JavaConversions._

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Apr-2016
  */
private[gittrunkdiff] object Utils {
  private val MASTER_NAMES: Array[String] = Array("svn/trunk", "master")

  /**
    * @return whether this plugin is valid for this project
    */
  def validForProject(implicit project: Project): Boolean = !project.isDisposed && gitVcsIsActive(project)

  type GitReposGetter = () => Array[GitRepository]

  def getGitRepos(implicit project: Project): GitReposGetter = {
    val gitVcs = GitVcs.getInstance(project)
    val projectVcsManager: ProjectLevelVcsManager = ProjectLevelVcsManager.getInstance(project)

    () => projectVcsManager.getAllVcsRoots
      .filter(_.getVcs.equals(gitVcs))
      .map((gitVcsRoot: VcsRoot) => {
        val repoRoot: VirtualFile = gitVcsRoot.getPath

          GitRepositoryImpl.getInstance(repoRoot, project, true)
        })
  }

  def getGitRepoForFile(file: VirtualFile)(implicit project: Project): Option[GitRepository] = {
    Option(GitUtil.getRepositoryManager(project).getRepositoryForFile(file))
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

  def getGitRepo(rootPath: VirtualFile)(implicit project: Project): GitRepository = GitRepositoryImpl.getInstance(rootPath, project, true)

  /**
    * @return whether git vcs is active for this project
    */
  private def gitVcsIsActive(project: Project): Boolean = {
    val gitVcs: GitVcs = GitVcs.getInstance(project)

    ProjectLevelVcsManager.getInstance(project).checkVcsIsActive(gitVcs)
  }
}