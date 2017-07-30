package com.kristofszilagyi.shared.macros

import utest._



@AutoWrapper
final case class Y(self: Int)


object AutoWrapperTest extends TestSuite {
  def tests = TestSuite {
    'test {
      val y = Y(1)
      println(y.intern)
    }
  }
}
