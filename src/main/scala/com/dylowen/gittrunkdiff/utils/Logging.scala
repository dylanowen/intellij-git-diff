package com.dylowen.gittrunkdiff.utils

import com.intellij.openapi.diagnostic.Logger

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Oct-2016
  */
trait Logging {
  protected val logger: Logger = Logger.getInstance(this.getClass)
}
