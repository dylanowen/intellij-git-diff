package com.dylowen.gittrunkdiff.editor.listener

import com.intellij.openapi.editor.event.{EditorFactoryEvent, EditorFactoryListener}
import com.intellij.openapi.editor.{Document, Editor}
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
  * com.intellij.openapi.vcs.impl.LineStatusTrackerManager.MyEditorFactoryListener
  * Watch the editors for changes and update the line trackers when they occur
  *
  * @author dylan.owen
  * @since Oct-2016
  */

private[editor] class LineTrackerEditorListener(/*private val editorManager: EditorManager,*/ private val project: Project) extends EditorFactoryListener {
  override def editorCreated(event: EditorFactoryEvent): Unit = {
    val editor: Editor = event.getEditor

    // check if the editor is for our project
    if (editor.getProject == null || editor.getProject.equals(this.project)) {
      val document: Document = editor.getDocument
      val virtualFile: VirtualFile = FileDocumentManager.getInstance.getFile(document)

      if (virtualFile != null) {
        //this.editorManager.installTracker(virtualFile, document)
      }
    }
  }

  override def editorReleased(event: EditorFactoryEvent): Unit = {
    val editor: Editor = event.getEditor
    val editorProject: Project = editor.getProject

    // check if the editor isn't for our project
    if (editorProject != null && editorProject != this.project) {
      return
    }

    val document: Document = editor.getDocument

    // not sure what this is for, maybe checking if the editor has actually been released?
    // com.intellij.openapi.vcs.impl.LineStatusTrackerManager.MyEditorFactoryListener.editorReleased
    val editors: Array[Editor] = event.getFactory.getEditors(document, this.project)
    if (editors.isEmpty || (editors.length == 1 && editor == editors(0))) {
      //this.editorManager.uninstallTracker(document)
    }
  }
}
