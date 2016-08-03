package com.dylowen.gittrunkdiff

import javax.swing.JComponent

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ui.ChangesViewContentProvider


/**
 * TODO add description
 *
 * @author dylan.owen
 * @since Jul-2016
 */
/** {@link CommittedChangesViewManager} */

class VcsTab(project: Project) extends ChangesViewContentProvider {

  var gitDiffView: Option[GitDiffView] = None

  override def initContent(): JComponent = {
    val gitDiffView = new GitDiffView(project)
    this.gitDiffView = Some(gitDiffView)

    gitDiffView
  }

  override def disposeContent(): Unit = {
    this.gitDiffView.foreach(_.disposeContent())

    this.gitDiffView = None
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




}