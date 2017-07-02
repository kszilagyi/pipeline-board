package com.kristofszilagyi.utils

import utest._
import com.kristofszilagyi.utils.FutureUtils.RichFuture

import scala.concurrent.Future
import scala.util.{Failure, Success}
import utest.framework.{Test, Tree}
import SameThreadExecutionContext._

object FutureUtilsTest extends TestSuite {
  def tests: Tree[Test] = this{
    'testSucceed {
      Future.successful(1).lift.value ==> Some(Success(Success(1)))
    }

    'testFail {
      val x = new RuntimeException("ada")
      Future.failed(x).lift.value ==> Some(Success(Failure(x)))
    }
  }
}
