package com.dylowen.gittrunkdiff

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem._
import com.intellij.openapi.project.{DumbAwareAction, Project}
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.content.{Content, ContentFactory}

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Apr-2016
  */
object GitDiffView {
  def getInstance(project: Project): GitDiffView = project.getComponent(classOf[GitDiffView])
}

class GitDiffView {

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

    content
  }

  private class RefreshAction() extends DumbAwareAction("Refresh", "Refresh the git stuff", AllIcons.Actions.Refresh) {
    override def actionPerformed(e: AnActionEvent): Unit = {
      System.out.println("Refreshed")
    }
  }


}
