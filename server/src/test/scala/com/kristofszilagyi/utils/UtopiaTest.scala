package com.kristofszilagyi.utils

import utest.framework.{Test, Tree}
import utest._
import SameThreadExecutionContext._
import com.kristofszilagyi.shared.Wart
import com.kristofszilagyi.utils.AssertionEx.fail
import java.lang
import scala.util.Try


@SuppressWarnings(Array(Wart.Throw))
object UtopiaTest extends TestSuiteWithLogging {
  def tests: Tree[Test] = this {
    'hande {
      val ex = new lang.AssertionError("mapFail")
      Utopia.finished(1).map(_ => ex).value ==> Try(1)
    }

  }
}
