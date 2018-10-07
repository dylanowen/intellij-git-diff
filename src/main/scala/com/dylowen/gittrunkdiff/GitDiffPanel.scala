package com.dylowen.gittrunkdiff

import java.awt.BorderLayout

import com.dylowen.gittrunkdiff.utils.Logging
import com.intellij.icons.AllIcons
import com.intellij.ide.{CommonActionsManager, TreeExpander}
import com.intellij.openapi.actionSystem.{ActionManager, ActionPlaces, AnActionEvent, DefaultActionGroup}
import com.intellij.openapi.project.{DumbAwareAction, Project}
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.vcs.changes.ui.{ChangesListView, TreeModelBuilder}
import com.intellij.openapi.vcs.changes.{ChangeList, ChangeListManagerImpl}
import com.intellij.ui.ScrollPaneFactory
import com.intellij.util.ui.tree.TreeUtil
import javax.swing.{JPanel, JScrollPane}

import scala.collection.JavaConverters._

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Oct-2018
  */
class GitDiffPanel(scheduleRefresh: () => Unit)
                  (implicit private val project: Project) extends SimpleToolWindowPanel(false, true) with Logging {

  private val changesView: ChangesListView = new ChangesListView(project)
  private val expander: ChangesExpander = new ChangesExpander()

  // build our panel
  {
    val toolbarPanel: JPanel = new JPanel(new BorderLayout())
    //this.changesView.setMenuActions(ActionManager.getInstance.getAction("ChangesViewPopupMenu").asInstanceOf[DefaultActionGroup])
    val scrollPane: JScrollPane = ScrollPaneFactory.createScrollPane(changesView)

    setContent(scrollPane)

    val visualActionsGroup: DefaultActionGroup = new DefaultActionGroup()

    visualActionsGroup.add(new RefreshAction())
    visualActionsGroup.add(CommonActionsManager.getInstance.createExpandAllAction(expander, toolbarPanel))
    visualActionsGroup.add(CommonActionsManager.getInstance.createCollapseAllAction(expander, toolbarPanel))
    //val showFlattenAction: ChangesViewManager#ToggleShowFlattenAction = new ChangesViewManager#ToggleShowFlattenAction
    //showFlattenAction.registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_P, ctrlMask)), panel)
    //visualActionsGroup.add(showFlattenAction)

    //implement this for showing the diff
    //visualActionsGroup.add(new ChangesViewManager#ToggleDetailsAction)

    toolbarPanel.add(ActionManager.getInstance.createActionToolbar(ActionPlaces.CHANGES_VIEW_TOOLBAR, visualActionsGroup, false).getComponent, BorderLayout.CENTER)

    setToolbar(toolbarPanel)

    scheduleRefresh()
  }

  def refresh(existingChanges: ChangeListManagerImpl, changeLists: Seq[ChangeList]) {
    //this.expander

    val treeModelBuilder: TreeModelBuilder = new TreeModelBuilder(project, changesView.getGrouping)
      .setChangeLists(changeLists.asJava, false)
      .setLocallyDeletedPaths(existingChanges.getDeletedFiles)
      .setModifiedWithoutEditing(existingChanges.getModifiedWithoutEditing)
      //.setSwitchedFiles(existingChanges.getSwitchedFilesMap)
      //.setSwitchedRoots(changeListManager.getSwitchedRoots())
      .setLockedFolders(existingChanges.getLockedFolders)
      //.setLogicallyLockedFiles(changeListManager.getLogicallyLockedFolders())
      .setUnversioned(existingChanges.getUnversionedFiles)

    changesView.updateModel(treeModelBuilder.build())

    expander.expandAll()
    //changeDetails()
  }

  private class ChangesExpander extends TreeExpander {
    override def expandAll(): Unit = TreeUtil.expandAll(changesView)

    override def collapseAll(): Unit = {
      TreeUtil.collapseAll(changesView, 2)
      TreeUtil.expand(changesView, 1)
    }

    override def canCollapse: Boolean = true

    override def canExpand: Boolean = true
  }

  private class RefreshAction() extends DumbAwareAction("Refresh", "Refresh Git", AllIcons.Actions.Refresh) {
    override def actionPerformed(e: AnActionEvent): Unit = scheduleRefresh()
  }

}