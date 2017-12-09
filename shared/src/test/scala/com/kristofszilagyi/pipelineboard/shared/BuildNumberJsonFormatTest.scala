package com.kristofszilagyi.pipelineboard.shared

import io.circe.JsonNumber
import utest._
import utest.framework.{Test, Tree}
import io.circe.syntax._
import io.circe.parser.decode


class BuildNumberJsonFormatTest extends TestSuite {

  def tests: Tree[Test] = this {
    'encode {
      BuildNumber(2).asJson ==> JsonNumber.fromIntegralStringUnsafe("2")
    }

    'decode {
      decode[BuildNumber]("2") ==> BuildNumber(2)
    }
  }
}
