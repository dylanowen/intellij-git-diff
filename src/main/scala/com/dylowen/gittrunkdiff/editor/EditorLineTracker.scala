package com.dylowen.gittrunkdiff.editor

import com.dylowen.gittrunkdiff.GitActions
import com.dylowen.gittrunkdiff.utils.JavaConversions._
import com.dylowen.gittrunkdiff.utils.{Logging, Utils}
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.{Application, ModalityState}
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.editor.{Document, EditorFactory}
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vcs.ex.LineStatusTracker
import com.intellij.openapi.vcs.history.VcsRevisionNumber
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList
import com.intellij.openapi.vcs.{AbstractVcs, FileStatus, FileStatusManager, ProjectLevelVcsManager}
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
//import com.intellij.util.concurrency.QueueProcessorRemovePartner
import git4idea.GitVcs
import net.jcip.annotations.GuardedBy

import scala.collection.concurrent.TrieMap
import scala.collection.mutable

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Aug-2016
  */
/*
private[editor] trait EditorManager {
  private[editor] def installTracker(file: VirtualFile, document: Document): Unit
  private[editor] def uninstallTracker(document: Document): Unit
  private[editor] def resetTracker(file: VirtualFile): Unit
}

object EditorLineTracker extends Logging
class EditorLineTracker(private[editor] val project: Project,
                        private val vcsManager: ProjectLevelVcsManager,
                        private val application: Application)
  extends ProjectComponent with Disposable with EditorManager {

  private[editor] val trackersLock = new Object

  @GuardedBy("trackersLock")
  val trackers: mutable.Map[Document, LineStatusTracker] = new TrieMap[Document, LineStatusTracker]()
  @GuardedBy("trackersLock")
  val revisionQueue: QueueProcessorRemovePartner[Document, RevisionLoader] = new QueueProcessorRemovePartner(this.project, (revision: RevisionLoader) => revision.load())

  implicit val implProject: Project = project

  Disposer.register(this.project, this)

  override def projectOpened(): Unit = if (Utils.validForProject) {
    StartupManager.getInstance(this.project).registerPreStartupActivity(() => {
      val editorFactory: EditorFactory = EditorFactory.getInstance
      val editorListener: LineTrackerEditorListener = new LineTrackerEditorListener(this, this.project)
      editorFactory.addEditorFactoryListener(editorListener, this.project)
    })
  }

  override private[editor] def installTracker(file: VirtualFile, document: Document): Unit = {
    // do our simple checks without a lock
    if (isDisabled || !shouldInstallTracker(file) || this.trackers.contains(document)) {
      return
    }

    this.trackersLock.synchronized({
      if (this.trackers.contains(document)) {
        return
      }

      val tracker = LineStatusTracker.createOn(file, document, this.project,  LineStatusTracker.Mode.DEFAULT)
      this.trackers.put(document, tracker)

      // start our revision loader
      this.revisionQueue.add(document, new RevisionLoader(file, document))
    })
  }

  //com.intellij.openapi.vcs.impl.LineStatusTrackerManager.releaseTracker
  override private[editor] def uninstallTracker(document: Document): Unit = {
    this.trackersLock.synchronized({
      if (isDisabled) {
        return
      }

      this.revisionQueue.remove(document)
      this.trackers.remove(document).foreach(_.release())
    })
  }

  override private[editor] def resetTracker(file: VirtualFile): Unit = ???

  private def isDisabled: Boolean = !this.project.isOpen || this.project.isDisposed

  /**
    * This is called if we should have a Tracker, so we try to find one and log some errors if we can't
    *
    * @param document the document to get the tracker for
    * @return
    */
  private def getAndValidateTracker(document: Document): Option[LineStatusTracker] = {
    val tracker: Option[LineStatusTracker] = EditorLineTracker.this.trackers.get(document)

    if (tracker.isEmpty) {
      log("Tracker has been released")

      return tracker
    }

    tracker
  }

  private def shouldInstallTracker(virtualFile: VirtualFile): Boolean = {
    if (isDisabled) {
      return false
    }

    if (virtualFile == null || virtualFile.isInstanceOf[LightVirtualFile]) {
      return false
    }
    if (!virtualFile.isInLocalFileSystem) {
      return false
    }

    val statusManager: FileStatusManager = FileStatusManager.getInstance(EditorLineTracker.this.project)
    if (statusManager == null) {
      return false
    }

    val activeVcs: AbstractVcs[_ <: CommittedChangeList] = EditorLineTracker.this.vcsManager.getVcsFor(virtualFile)
    if (activeVcs == null || !activeVcs.isInstanceOf[GitVcs]) {
      log("shouldBeInstalled failed: no active VCS", virtualFile)

      return false
    }

    val status: FileStatus = statusManager.getStatus(virtualFile)
    if ((status eq FileStatus.ADDED) || (status eq FileStatus.UNKNOWN) || (status eq FileStatus.IGNORED)) {
      log("shouldBeInstalled skipped: status=" + status, virtualFile)
      return false
    }
    true
  }

  override def dispose(): Unit = {
    this.trackersLock.synchronized({
      this.trackers.foreach({case (_, tracker) => tracker.release()})

      this.trackers.clear()
      this.revisionQueue.clear()
    })
  }

  private def log(message: String, file: VirtualFile = null) {
    // append the file if it isn't null
    EditorLineTracker.log(message + (if (file != null) "; file: " + file.getPath else ""))
  }

  override def getComponentName: String = getClass.getName
  override def projectClosed(): Unit = {}
  override def initComponent(): Unit = {}
  override def disposeComponent(): Unit = {}

  class RevisionLoader(val file: VirtualFile, val document: Document) {
    //implicit val project: Project = editorTracker.project

    def load(): Unit = {
      // check before synchronize
      if (EditorLineTracker.this.getAndValidateTracker(document).isDefined) {
        EditorLineTracker.this.trackersLock.synchronized({
          if (EditorLineTracker.this.getAndValidateTracker(document).isDefined) {
            Utils.getGitRepoForFile(this.file) match {
              case Some(gitRepo) =>
                // find the revision number where we branched from master
                val revisionNumber: VcsRevisionNumber = GitActions.getRevisionWhenBranched(gitRepo)
                val fileAtRevision: String = GitActions.getFileAtRevision(file, revisionNumber, gitRepo)

                nonModalAliveInvokeLater(fileAtRevision)
              case None => EditorLineTracker.this.log("Couldn't find repo for file", file)
            }
          }
        })
      }
    }

    private def nonModalAliveInvokeLater(fileAtRevision: String): Unit = {
      EditorLineTracker.this.application.invokeLater(() => {
        // synchronize
        EditorLineTracker.this.trackersLock.synchronized({
          // pull out the tracker and set it's base revision
          EditorLineTracker.this.getAndValidateTracker(this.document).foreach(_.setBaseRevision(fileAtRevision))
        })
      }, ModalityState.NON_MODAL, () => EditorLineTracker.this.isDisabled)
    }
  }
}*/