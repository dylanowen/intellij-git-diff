package com.dylowen.gittrunkdiff

import java.awt.BorderLayout
import java.util
import javax.swing.JPanel

import com.intellij.ide.{CommonActionsManager, TreeExpander}
import com.intellij.openapi.actionSystem._
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.vcs.changes.ui.{ChangesListView, TreeModelBuilder}
import com.intellij.openapi.vcs.changes.{Change, ChangeList, ChangeListManagerImpl}
import com.intellij.openapi.vcs.{ProjectLevelVcsManager, VcsRoot}
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.MultiMap
import com.intellij.util.ui.tree.TreeUtil
import git4idea.changes.GitChangeUtils
import git4idea.commands.{GitCommand, GitSimpleHandler}
import git4idea.repo.{GitRepository, GitRepositoryImpl}
import git4idea.{GitBranch, GitRevisionNumber, GitVcs}

import scala.collection.JavaConversions._
/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Apr-2016
  */
//http://www.jetbrains.org/intellij/sdk/docs/reference_guide/vcs_integration_for_plugins.html

//GitCompareWithBranchAction

object GitDiffView {
  //def getInstance(project: Project): GitDiffView = project.getComponent(classOf[GitDiffView])
  private val LOG: Logger = Logger.getInstance(GitDiffView.getClass)
}


//com.intellij.openapi.vcs.changes.ChangesViewManager
class GitDiffView(project: Project) extends SimpleToolWindowPanel(false, true) {
  var disposed = false

  val gitVcs = GitVcs.getInstance(this.project)
  val projectVcsManager: ProjectLevelVcsManager = ProjectLevelVcsManager.getInstance(this.project)

  val changesView = new ChangesListView(this.project)
  val toolbarPanel: JPanel = new JPanel(new BorderLayout())

  //val progressLabel = new JPanel(new BorderLayout())
  //toolbarPanel.add(toolbarComponent, BorderLayout.WEST)

  setContent(this.changesView)


  // Setup Toolbar
  val visualActionsGroup: DefaultActionGroup = new DefaultActionGroup()
  val expander = new ChangesExpander()
  visualActionsGroup.add(CommonActionsManager.getInstance.createExpandAllAction(expander, toolbarPanel))
  visualActionsGroup.add(CommonActionsManager.getInstance.createCollapseAllAction(expander, toolbarPanel))
  //val showFlattenAction: ChangesViewManager#ToggleShowFlattenAction = new ChangesViewManager#ToggleShowFlattenAction
  //showFlattenAction.registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_P, ctrlMask)), panel)
  //visualActionsGroup.add(showFlattenAction)

  //implement this for showing the diff
  ///visualActionsGroup.add(new ChangesViewManager#ToggleDetailsAction)

  toolbarPanel.add(ActionManager.getInstance.createActionToolbar(ActionPlaces.CHANGES_VIEW_TOOLBAR, visualActionsGroup, false).getComponent, BorderLayout.CENTER)

  this.setToolbar(toolbarPanel)

  refreshView()
  expander.expandAll()

  private def gitMergeBase(branchA: GitBranch, branchB: GitBranch, root: VirtualFile): GitRevisionNumber = gitMergeBase(branchA.getFullName, branchB.getFullName, root)

  private def gitMergeBase(branchA: String, branchB: String, root: VirtualFile): GitRevisionNumber = {
    val handler: GitSimpleHandler = new GitSimpleHandler(this.project, root, GitCommand.MERGE_BASE)

    handler.addParameters(branchA, branchB)
    handler.setSilent(true)
    handler.setStdoutSuppressed(true)

    val revisionString: String = handler.run().trim

    GitRevisionNumber.resolve(this.project, root, revisionString)
  }

  //GitCompareWithBranchAction
  private def getLastBranchRevision(currentBranch: GitBranch, gitRepo: GitRepository): GitRevisionNumber = {
    val localBranches = gitRepo.getBranches.getLocalBranches.filter(!_.equals(currentBranch))

    var latestRevision: GitRevisionNumber = gitMergeBase(localBranches.head, currentBranch, gitRepo.getRoot)
    for (branch: GitBranch <- localBranches.tail) {
      val revision: GitRevisionNumber = gitMergeBase(branch, currentBranch, gitRepo.getRoot)

      if (latestRevision.compareTo(revision) < 0) {
        latestRevision = revision
      }
    }

    latestRevision
  }






  private def getGitChangesSinceBranch(): Array[ChangeList] = {
    val gitVcsRoots: Array[VcsRoot] = projectVcsManager.getAllVcsRoots.filter(_.getVcs.equals(gitVcs))

    //TODO there has to be a better way to do this
    val changes: Array[Option[ChangeList]] = for (gitVcsRoot: VcsRoot <- gitVcsRoots) yield {
      val repoRoot: VirtualFile = gitVcsRoot.getPath
      val gitRepo: GitRepository = GitRepositoryImpl.getInstance(repoRoot, this.project, true)
      val currentBranch: GitBranch = gitRepo.getCurrentBranch

      //TODO fix how we guess at the master branch
      if (currentBranch.getName.equals("master") || currentBranch.getName.equals("svn/trunk")) {
        None
      }
      else {
        val lastBranchRevision: GitRevisionNumber = getLastBranchRevision(currentBranch, gitRepo)
        val changes = GitChangeUtils.getDiff(this.project, repoRoot, lastBranchRevision.getRev, currentBranch.getName, null)

        Some(new GitBranchChangeList(currentBranch.getName, currentBranch.getFullName, changes))
      }
    }

    changes.filter(_.isDefined).map(_.get)
  }
  //val currentRevision = new GitRevisionNumber(branch)



  private def refreshView() {
    if (this.disposed || !this.project.isInitialized || ApplicationManager.getApplication.isUnitTestMode || !Utils.validForProject(project)) {
      return
    }

    val changeListManager: ChangeListManagerImpl = ChangeListManagerImpl.getInstanceImpl(this.project)

    val changeLists: Array[ChangeList] = getGitChangesSinceBranch()

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
    //changeDetails()
  }

  def disposeContent(): Unit = {
    this.disposed = true
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



  class ChangesExpander extends TreeExpander {
    override def expandAll(): Unit = TreeUtil.expandAll(GitDiffView.this.changesView)

    override def collapseAll(): Unit = {
      TreeUtil.collapseAll(GitDiffView.this.changesView, 2)
      TreeUtil.expand(GitDiffView.this.changesView, 1)
    }

    override def canCollapse: Boolean = true

    override def canExpand: Boolean = true
  }

  class GitBranchChangeList(val name: String, val comment: String, val changes: util.Collection[Change]) extends ChangeList {
    override def getName: String = name
    override def getComment: String = comment
    override def getChanges: util.Collection[Change] = changes
  }
}

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

  private class RefreshAction() extends DumbAwareAction("Refresh", "Refresh Git", AllIcons.Actions.Refresh) {
    override def actionPerformed(e: AnActionEvent): Unit = GitDiffView.this.refresh()
  }
}
*/