package com.kristofszilagyi.utils

import utest.TestSuite
import utest.framework.{Test, Tree}
import com.kristofszilagyi.utils.FutureUtils.RichFuture

import scala.concurrent.Future
import scala.util.Success
import TypeSafeEqualsOps._

class FutureUtilsTest extends TestSuite {
  def tests: Tree[Test] = {
    this {
      'testSucceed {
        assert(Future.successful(1).lift ==== Future(Success(1)))
      }
    }
  }
}
