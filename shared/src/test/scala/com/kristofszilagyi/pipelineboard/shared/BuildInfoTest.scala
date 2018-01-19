package com.kristofszilagyi.pipelineboard.shared

import java.time.Instant

import utest._
import utest.framework.{Test, Tree}

object BuildInfoTest extends TestSuite {

  def tests: Tree[Test] = this {
    'overlapFirstLeft {
      val b1 = BuildInfo.successful(Instant.ofEpochMilli(10), Instant.ofEpochMilli(20), BuildNumber(1))
      val b2 = BuildInfo.successful(Instant.ofEpochMilli(15), Instant.ofEpochMilli(25), BuildNumber(2))
      b1.overlap(b2) ==> true
    }

    'overlapFirstRight {
      val b1 = BuildInfo.successful(Instant.ofEpochMilli(10), Instant.ofEpochMilli(20), BuildNumber(1))
      val b2 = BuildInfo.successful(Instant.ofEpochMilli(15), Instant.ofEpochMilli(25), BuildNumber(2))
      b2.overlap(b1) ==> true
    }

    'noOverLapFirstRight {
      val b1 = BuildInfo.successful(Instant.ofEpochMilli(10), Instant.ofEpochMilli(20), BuildNumber(1))
      val b2 = BuildInfo.successful(Instant.ofEpochMilli(25), Instant.ofEpochMilli(30), BuildNumber(2))
      b1.overlap(b2) ==> false
    }

    'noOverLapFirstLeft {
      val b1 = BuildInfo.successful(Instant.ofEpochMilli(10), Instant.ofEpochMilli(20), BuildNumber(1))
      val b2 = BuildInfo.successful(Instant.ofEpochMilli(25), Instant.ofEpochMilli(30), BuildNumber(2))
      b2.overlap(b1) ==> false
    }
  }
}
