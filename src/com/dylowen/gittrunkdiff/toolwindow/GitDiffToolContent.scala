package com.dylowen.gittrunkdiff.toolwindow

import java.awt.BorderLayout
import java.util
import javax.swing.{JPanel, JScrollPane}

import com.dylowen.gittrunkdiff.settings.ProjectSettings
import com.dylowen.gittrunkdiff.utils.{GitActions, Utils}
import com.dylowen.gittrunkdiff.utils.Utils.GitReposGetter
import com.intellij.icons.AllIcons
import com.intellij.ide.{CommonActionsManager, TreeExpander}
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem._
import com.intellij.openapi.application.{ApplicationManager, ModalityState}
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.{DumbAwareAction, Project}
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vcs.changes._
import com.intellij.openapi.vcs.changes.ui.{ChangesListView, TreeModelBuilder}
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.ScrollPaneFactory
import com.intellij.util.Alarm
import com.intellij.util.containers.MultiMap
import com.intellij.util.ui.tree.TreeUtil
import git4idea.changes.GitChangeUtils
import git4idea.repo.GitRepository
import git4idea.{GitBranch, GitRevisionNumber}

import scala.collection.JavaConversions._
import scala.util.Try
/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Apr-2016
  */
//http://www.jetbrains.org/intellij/sdk/docs/reference_guide/vcs_integration_for_plugins.html
//GitCompareWithBranchAction
object GitDiffToolContent {
  //def getInstance(project: Project): GitDiffView = project.getComponent(classOf[GitDiffView])
  private val LOG: Logger = Logger.getInstance(GitDiffToolContent.getClass)
}

//com.intellij.openapi.vcs.changes.ChangesViewManager
class GitDiffToolContent()(implicit val project: Project) extends SimpleToolWindowPanel(false, true) {
  private var disposed = false

  private val gitVcsRootsGetter: GitReposGetter = Utils.getGitRepos

  private val expander = new ChangesExpander()
  private val changesView = new ChangesListView(this.project)
  private val toolbarPanel: JPanel = new JPanel(new BorderLayout())

  private val repaintAlarm: Alarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, this.project)
  private val changeListener: ChangeListListener = new GitDiffChangelistListener()

  {
    this.changesView.setMenuActions(ActionManager.getInstance.getAction("ChangesViewPopupMenu").asInstanceOf[DefaultActionGroup])
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
    ///visualActionsGroup.add(new ChangesViewManager#ToggleDetailsAction)

    toolbarPanel.add(ActionManager.getInstance.createActionToolbar(ActionPlaces.CHANGES_VIEW_TOOLBAR, visualActionsGroup, false).getComponent, BorderLayout.CENTER)

    this.setToolbar(toolbarPanel)

    val changeListManager: ChangeListManager = ChangeListManager.getInstance(this.project)
    changeListManager.addChangeListListener(this.changeListener)
    Disposer.register(this.project, new Disposable() {
      def dispose() {
        changeListManager.removeChangeListListener(GitDiffToolContent.this.changeListener)
      }
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

    refreshView()
  }

  private def getGitChangesSinceBranches: Array[ChangeList] = {
    val gitRepos: Array[GitRepository] = gitVcsRootsGetter()

    val changes: Array[Option[ChangeList]] = for (gitRepo: GitRepository <- gitRepos) yield {
      GitActions.getChangesSinceBranch(gitRepo)
    }

    changes.filter(_.isDefined).map(_.get)
  }

  private def refreshView() {
    if (!(this.disposed || !this.project.isInitialized || ApplicationManager.getApplication.isUnitTestMode || !Utils.validForProject(project))) {

      //this.expander

      val changeListManager: ChangeListManagerImpl = ChangeListManagerImpl.getInstanceImpl(this.project)

      val gitChangeLists: Array[ChangeList] = getGitChangesSinceBranches
      val changeLists: Array[ChangeList] = (changeListManager.getChangeListsCopy ++ gitChangeLists).toArray

      this.changesView.updateModel(new TreeModelBuilder(this.project, this.changesView.isShowFlatten)
        .set(
          changeLists.toSeq,
          changeListManager.getDeletedFiles,
          changeListManager.getModifiedWithoutEditing,
          new MultiMap[String, VirtualFile](), //changeListManager.getSwitchedFilesMap,
          null, //changeListManager.getSwitchedRoots,
          null,
          changeListManager.getLockedFolders,
          null //changeListManager.getLogicallyLockedFolders
        )
        .setUnversioned(changeListManager.getUnversionedFiles, changeListManager.getUnversionedFilesSize)
        .build
      )

      expander.expandAll()
      //changeDetails()
    }
  }

  def scheduleRefresh(): Unit = {
    if (!(this.disposed || ApplicationManager.getApplication.isHeadlessEnvironment || this.project.isDisposed || !Utils.validForProject(project))) {
      val was: Int = repaintAlarm.cancelAllRequests
      if (GitDiffToolContent.LOG.isDebugEnabled) {
        GitDiffToolContent.LOG.debug("schedule refresh, was " + was)
      }
      if (!this.repaintAlarm.isDisposed) {
        this.repaintAlarm.addRequest(new Runnable() {
          def run() {
            refreshView()
          }
        }, 100, ModalityState.NON_MODAL)
      }
    }
  }

  def disposeContent(): Unit = {
    this.disposed = true
    this.repaintAlarm.cancelAllRequests
  }

  private class ChangesExpander extends TreeExpander {
    override def expandAll(): Unit = TreeUtil.expandAll(GitDiffToolContent.this.changesView)

    override def collapseAll(): Unit = {
      TreeUtil.collapseAll(GitDiffToolContent.this.changesView, 2)
      TreeUtil.expand(GitDiffToolContent.this.changesView, 1)
    }

    override def canCollapse: Boolean = true

    override def canExpand: Boolean = true
  }

  private class RefreshAction() extends DumbAwareAction("Refresh", "Refresh Git", AllIcons.Actions.Refresh) {
    override def actionPerformed(e: AnActionEvent): Unit = GitDiffToolContent.this.refreshView()
  }

  private class GitDiffChangelistListener extends ChangeListListener {
    override def changeListAdded(list: ChangeList): Unit = scheduleRefresh()

    override def changeListChanged(list: ChangeList): Unit = scheduleRefresh()

    override def changeListCommentChanged(list: ChangeList, oldComment: String): Unit = scheduleRefresh()

    override def changeListRemoved(list: ChangeList): Unit = scheduleRefresh()

    override def changeListRenamed(list: ChangeList, oldName: String): Unit = scheduleRefresh()

    override def changeListUpdateDone(): Unit = scheduleRefresh()

    override def changesAdded(changes: util.Collection[Change], toList: ChangeList): Unit = scheduleRefresh()

    override def changesMoved(changes: util.Collection[Change], fromList: ChangeList, toList: ChangeList): Unit = scheduleRefresh()

    override def changesRemoved(changes: util.Collection[Change], fromList: ChangeList): Unit = scheduleRefresh()

    override def defaultListChanged(oldDefaultList: ChangeList, newDefaultList: ChangeList): Unit = scheduleRefresh()

    override def unchangedFileStatusChanged(): Unit = scheduleRefresh()
  }
}
  /*
  EmptyAction.registerWithShortcutSet("ChangesView.Refresh", CommonShortcuts.getRerun, this)
  EmptyAction.registerWithShortcutSet("ChangesView.Diff", CommonShortcuts.getDiff, this)

  val group: DefaultActionGroup = ActionManager.getInstance.getAction("ChangesViewToolbar").asInstanceOf[DefaultActionGroup]

  val toolbar: ActionToolbar = ActionManager.getInstance.createActionToolbar(ActionPlaces.CHANGES_VIEW_TOOLBAR, group, false)
  toolbar.setTargetComponent(changesView)
  val toolbarComponent: JComponent = toolbar.getComponent
  val toolbarPanel: JPanel = new JPanel(new BorderLayout)
  toolbarPanel.add(toolbarComponent, BorderLayout.WEST)
  val content: JPanel = new JPanel(new BorderLayout)


  val splitter = new JBSplitter(false, "ChangesViewManager.DETAILS_SPLITTER_PROPORTION", 0.5f)
  splitter.setHonorComponentsMinimumSize(false)
  val scrollPane: JScrollPane = ScrollPaneFactory.createScrollPane(changesView)
  val wrapper: JPanel = new JPanel(new BorderLayout)
  wrapper.add(scrollPane, BorderLayout.CENTER)
  splitter.setFirstComponent(wrapper)
  content.add(splitter, BorderLayout.CENTER)
  content.add(progressLabel, BorderLayout.SOUTH)*/






  /*
  private def createViewInContentPanel(): Content = {

    /
    val panel: SimpleToolWindowPanel = new SimpleToolWindowPanel(false, true)

    EmptyAction.registerWithShortcutSet("ChangesView.Refresh", CommonShortcuts.getRerun, panel)
    EmptyAction.registerWithShortcutSet("ChangesView.Diff", CommonShortcuts.getDiff, panel)


  } */


/*
class GitDiffView(val project: Project) {
  def init(toolWindow: ToolWindow): Unit = {
    val content: Content = createViewInContentPanel()

    toolWindow.getContentManager.addContent(content)
  }

  private def createViewInContentPanel(): Content = {
    val panel: SimpleToolWindowPanel = new SimpleToolWindowPanel(false, true)

    val content = ContentFactory.SERVICE.getInstance().createContent(panel, "", false)
    content.setCloseable(true)

    val group: DefaultActionGroup = new DefaultActionGroup()
    group.add(new RefreshAction())



    val toolbar: ActionToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, group, false)
    //toolbar.getComponent().addFocusListener(createFocusListener());
    toolbar.setTargetComponent(panel)
    panel.setToolbar(toolbar.getComponent)

    refresh()

    content
  }

  private def createVisualActionsGroup(panel: SimpleToolWindowPanel): Unit = {
    val visualActionsGroup: DefaultActionGroup = new DefaultActionGroup
    val expander: Expander = new Expander
    visualActionsGroup.add(CommonActionsManager.getInstance.createExpandAllAction(expander, panel))
    visualActionsGroup.add(CommonActionsManager.getInstance.createCollapseAllAction(expander, panel))

    /*
    val showFlattenAction: ChangesViewManager#ToggleShowFlattenAction = new ChangesViewManager#ToggleShowFlattenAction
    showFlattenAction.registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_P, ctrlMask)), panel)
    visualActionsGroup.add(showFlattenAction)
    visualActionsGroup.add(ActionManager.getInstance.getAction(IdeActions.ACTION_COPY))
    visualActionsGroup.add(new ChangesViewManager#ToggleShowIgnoredAction)
    visualActionsGroup.add(new IgnoredSettingsAction)
    myToggleDetailsAction = new ChangesViewManager#ToggleDetailsAction
    visualActionsGroup.add(myToggleDetailsAction)
    visualActionsGroup.add(new ContextHelpAction(ChangesListView.ourHelpId))
    toolbarPanel.add(ActionManager.getInstance.createActionToolbar(ActionPlaces.CHANGES_VIEW_TOOLBAR, visualActionsGroup, false).getComponent, BorderLayout.CENTER)
    */
  }

  private def refresh(): Unit = {
    System.out.println("Refreshed")

    val changeListManager: ChangeListManagerImpl = ChangeListManagerImpl.getInstanceImpl(project)
  }

  private def expandAll(): Unit = {

  }

  private def collapseAll(): Unit = {

  }

  private class Expander extends TreeExpander {
    override def expandAll(): Unit = GitDiffView.this.expandAll()
    override def collapseAll(): Unit = GitDiffView.this.collapseAll()
    override def canCollapse: Boolean = true
    override def canExpand: Boolean = true
  }


}
*/