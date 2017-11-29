package com.kristofszilagyi.pipelineboard.utils

import com.kristofszilagyi.pipelineboard.utils.SameThreadExecutionContext._
import com.kristofszilagyi.pipelineboard.utils.Utopia.RichFuture
import utest._
import utest.framework.{Test, Tree}

import scala.concurrent.Future
import scala.util.{Failure, Success}

object FutureUtilsTest extends TestSuite {
  def tests: Tree[Test] = this{
    'testSucceed {
      Future.successful(1).lift.value ==> Some(Success(1)) //this does not look lifted because it's a lifted future!
    }

    'testFail {
      val x = new RuntimeException("ada")
      Future.failed(x).lift.value ==> Some(Failure(x))
    }
  }
}
