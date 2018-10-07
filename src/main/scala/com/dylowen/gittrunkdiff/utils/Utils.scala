package com.dylowen.gittrunkdiff.utils

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.{ProjectLevelVcsManager, VcsRoot}
import com.intellij.openapi.vfs.VirtualFile
import git4idea._
import git4idea.repo.{GitRepository, GitRepositoryImpl}

import scala.collection.JavaConverters._

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Apr-2016
  */
private[gittrunkdiff] object Utils {

  private object BranchNames {
    val SvnTrunk: String = "svn/trunk"
    val Master: String = "master"
  }

  /**
    * @return whether this plugin is valid for this project
    */
  def validForProject(implicit project: Project): Boolean = !project.isDisposed && gitVcsIsActive(project)

  def getGitRepos(implicit project: Project): Seq[GitRepository] = {
    val gitVcs: GitVcs = GitVcs.getInstance(project)
    val projectVcsManager: ProjectLevelVcsManager = ProjectLevelVcsManager.getInstance(project)

    projectVcsManager.getAllVcsRoots.toSeq
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
    val branches: Iterable[GitLocalBranch] = repo.getBranches.getLocalBranches.asScala

    val masterBranch: Option[GitBranch] = branches
      // find svn/trunk first
      .find(_.getName == BranchNames.SvnTrunk)
      .orElse({
        // search for master
        branches.find(_.getName == BranchNames.Master)
      })
      .orElse({
        //fall back on any branch
        branches.headOption
      })

    // TODO fix null
    masterBranch.orNull
  }

  def getBranch(name: String, repo: GitRepository): Option[GitBranch] = Option(repo.getBranches.findLocalBranch(name))

  def getGitRepo(rootPath: VirtualFile)(implicit project: Project): GitRepository = GitRepositoryImpl.getInstance(rootPath, project, true)

  /**
    * @return whether git vcs is active for this project
    */
  private def gitVcsIsActive(implicit project: Project): Boolean = {
    val gitVcs: GitVcs = GitVcs.getInstance(project)

    ProjectLevelVcsManager.getInstance(project).checkVcsIsActive(gitVcs)
  }
}