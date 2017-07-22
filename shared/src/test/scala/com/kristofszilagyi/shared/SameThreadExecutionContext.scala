package com.kristofszilagyi.shared

import scala.concurrent.ExecutionContext

object SameThreadExecutionContext {
  implicit val ec = new ExecutionContext {
     def execute(runnable: Runnable): Unit = {
       runnable.run()
     }

     def reportFailure(cause: Throwable): Unit = {
      println(s"SameThreadExecutionContext.report: ${cause.getMessage}")
    }
  }
}
