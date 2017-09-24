package com.kristofszilagyi.utils

import java.lang

import com.kristofszilagyi.shared.Wart
import com.kristofszilagyi.utils.SameThreadExecutionContext._
import utest._
import utest.framework.{Test, Tree}

import scala.util.Failure


@SuppressWarnings(Array(Wart.Throw))
object UtopiaTest extends TestSuiteWithLogging {
  def tests: Tree[Test] = this {
    'hande {
      val ex = new lang.AssertionError("mapFail")
      Utopia.finished(1).map(_ => throw ex).value ==> Some(Failure(ex))
    }

  }
}
