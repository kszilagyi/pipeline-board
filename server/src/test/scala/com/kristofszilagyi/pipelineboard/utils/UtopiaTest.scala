package com.kristofszilagyi.pipelineboard.utils

import java.lang

import com.kristofszilagyi.pipelineboard.shared.Wart
import com.kristofszilagyi.pipelineboard.utils.SameThreadExecutionContext._
import utest._
import utest.framework.{Test, Tree}

import scala.util.Success


@SuppressWarnings(Array(Wart.Throw))
object UtopiaTest extends TestSuiteWithLogging {
  def tests: Tree[Test] = this {
    'hande {
      val ex = new lang.AssertionError("mapFail")
      Utopia.finished(1).map(_ => throw ex).value.map(_.failed.map(_.getCause)) ==> Some(Success(ex))
    }

  }
}
