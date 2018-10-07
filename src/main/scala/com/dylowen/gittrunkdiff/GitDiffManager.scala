package com.dylowen.gittrunkdiff

import com.dylowen.gittrunkdiff.utils.{Logging, Utils}
import com.intellij.openapi.application.{ApplicationManager, ModalityState}
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vcs.changes._
import com.intellij.util.Alarm

import scala.collection.JavaConverters._

/**
  *
  * References:
  *
  * http://www.jetbrains.org/intellij/sdk/docs/reference_guide/vcs_integration_for_plugins.html
  *
  * [[git4idea.actions.GitCompareWithBranchAction]]
  * [[com.intellij.openapi.vcs.changes.ChangesViewManager]]
  *
  * @author dylan.owen
  * @since Apr-2016
  */
object GitDiffManager {
  def apply(implicit project: Project): GitDiffManager = {
    new GitDiffManager()
  }
}

class GitDiffManager()(implicit val project: Project) extends Logging {
  @volatile
  private var disposed = false

  lazy val panel: GitDiffPanel = new GitDiffPanel(() => scheduleRefresh())

  private val changeListManager: ChangeListManagerImpl = ChangeListManagerImpl.getInstanceImpl(project)
  private val repaintAlarm: Alarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, this.project)
  private val changeListener: ChangeListListener = new ChangeListAdapter() {
    override def changeListsChanged(): Unit = scheduleRefresh()
  }

  // setup our listeners
  {
    changeListManager.addChangeListListener(changeListener)
    Disposer.register(this.project, () => {
      changeListManager.removeChangeListListener(changeListener)
    })

    /*
    this.project.getMessageBus.connect.subscribe(RemoteRevisionsCache.REMOTE_VERSION_CHANGED, new Runnable() {
      def run() {
        ApplicationManager.getApplication.invokeLater(new Runnable() {
          def run() {
            refreshView()
          }
        }, ModalityState.NON_MODAL, GitDiffView.this.project.getDisposed)
      }
    })
    */
  }

  /*
  def immediateRefresh(): Unit = {
    repaintAlarm.cancelAllRequests()

    refreshPanel()
  }
  */

  def scheduleRefresh(): Unit = {
    if (shouldRefresh) {
      val was: Int = repaintAlarm.cancelAllRequests()

      logger.debug("schedule refresh, was " + was)

      if (!repaintAlarm.isDisposed) {
        repaintAlarm.addRequest(() => {
          refreshPanel()
        }, 100, ModalityState.NON_MODAL)
      }
    }
  }

  def refreshPanel(): Unit = {
    if (shouldRefresh) {
      val gitChangeLists: Seq[ChangeList] = getGitChangesSinceBranches

      val changeLists: Seq[ChangeList] = changeListManager.getChangeListsCopy.asScala ++ gitChangeLists

      panel.refresh(changeListManager, changeLists)
    }
  }

  def disposeContent(): Unit = {
    disposed = true
    repaintAlarm.cancelAllRequests
  }

  private def getGitChangesSinceBranches: Seq[ChangeList] = {
    Utils.getGitRepos
      .map(GitActions.getChangesSinceBranch)
      .collect({
        case Some(changeList) => changeList
      })
  }

  private def shouldRefresh: Boolean = {
    !(disposed ||
      ApplicationManager.getApplication.isHeadlessEnvironment ||
      project.isDisposed ||
      !Utils.validForProject(project)
      )
  }
}