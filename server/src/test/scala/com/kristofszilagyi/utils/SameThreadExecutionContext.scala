package com.kristofszilagyi.utils

import scala.concurrent.ExecutionContext

object SameThreadExecutionContext {
  implicit val c: ExecutionContext= new ExecutionContext {
    def execute(runnable: Runnable): Unit = runnable.run()

    def reportFailure(cause: Throwable): Unit = println(cause.getMessage)
  }

}
