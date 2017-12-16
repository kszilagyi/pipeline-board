package com.kristofszilagyi.pipelineboard.utils
import java.time.{Instant, LocalDateTime, ZoneOffset}

import io.circe.syntax.EncoderOps
import utest._
import utest.framework.{Test, Tree}

object TeamCityInstantParserTest extends TestSuite {
  def tests: Tree[Test] = this {
    'basic {
      TeamCityInstantParser.decodeInstant.decodeJson("20171212T061850+0000".asJson) ==>
        Right(LocalDateTime.of(2017, 12, 12, 6, 18, 50).toInstant(ZoneOffset.UTC))
    }

  }
}
