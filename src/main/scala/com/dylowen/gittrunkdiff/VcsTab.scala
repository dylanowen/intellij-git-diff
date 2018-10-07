package com.dylowen.gittrunkdiff

import javax.swing.JComponent
import com.dylowen.gittrunkdiff.settings.ApplicationSettings
import com.dylowen.gittrunkdiff.utils.Utils
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ui.ChangesViewContentProvider
import com.intellij.util.NotNullFunction


/**
 * [[com.intellij.openapi.vcs.changes.committed.CommittedChangesViewManager]]
 *
 * @author dylan.owen
 * @since Jul-2016
 */
class VcsTab(implicit val project: Project) extends ChangesViewContentProvider {
  @volatile
  var gitDiffManager: Option[GitDiffManager] = None

  override def initContent(): JComponent = {
    val newManager = new GitDiffManager()
    this.gitDiffManager = Some(newManager)

    newManager.panel
  }

  override def disposeContent(): Unit = {
    this.gitDiffManager.foreach(_.disposeContent())

    this.gitDiffManager = None
  }
}

class ShowVcsTab extends NotNullFunction[Project, Boolean] {
  override def fun(project: Project): Boolean = Utils.validForProject(project) && !ApplicationSettings.showOwnToolbar
}

    /*
    val myBrowser = new CommittedChangesTreeBrowser(project, Collections.emptyList[CommittedChangeList])
    myBrowser.getEmptyText.setText(VcsBundle.message("incoming.changes.not.loaded.message"))
    val group: ActionGroup = ActionManager.getInstance.getAction("IncomingChangesToolbar").asInstanceOf[ActionGroup]
    val toolbar: ActionToolbar = myBrowser.createGroupFilterToolbar(project, group, null, Collections.emptyList[AnAction])
    myBrowser.setToolBar(toolbar.getComponent)
    myBrowser.setTableContextMenu(group, Collections.emptyList[AnAction])
    //val myConnection = myBus.connect
    //myConnection.subscribe(CommittedChangesCache.COMMITTED_TOPIC, new IncomingChangesViewProvider#MyCommittedChangesListener)
    //loadChangesToBrowser(false, true)
    */