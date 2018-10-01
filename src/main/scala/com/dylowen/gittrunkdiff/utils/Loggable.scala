package com.dylowen.gittrunkdiff.utils

import com.intellij.openapi.diagnostic.Logger

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Oct-2016
  */
trait Loggable {
  private val LOG: Logger = Logger.getInstance(this.getClass)

  def log(message: String): Unit = {
    if (LOG.isDebugEnabled) {
      LOG.debug(message)
    }
  }
}
