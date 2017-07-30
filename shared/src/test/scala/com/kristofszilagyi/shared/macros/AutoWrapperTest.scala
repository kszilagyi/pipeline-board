package com.kristofszilagyi.shared.macros

import com.kristofszilagyi.shared.Wart
import utest._



@AutoWrapper
@SuppressWarnings(Array(Wart.Overloading, Wart.StringPlusAny))
final case class Y(self: Int)


object AutoWrapperTest extends TestSuite {
  def tests = TestSuite {
    'test {
      val y = Y(1)
      println(y + 2)
    }
  }
}
