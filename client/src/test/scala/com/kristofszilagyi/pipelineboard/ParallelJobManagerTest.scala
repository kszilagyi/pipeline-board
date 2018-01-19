package com.kristofszilagyi.pipelineboard

import java.time.Instant

import com.kristofszilagyi.pipelineboard.ParallelJobManager.overlappingIslands
import com.kristofszilagyi.pipelineboard.shared.{BuildInfo, BuildNumber}
import utest._

object ParallelJobManagerTest  extends TestSuite {

  def tests = TestSuite {
    'OneElement {
      val buildInfo = BuildInfo.successful(Instant.ofEpochMilli(1), Instant.ofEpochMilli(2), BuildNumber(2))
      overlappingIslands(Traversable(buildInfo)) ==> Set(Island(Seq(buildInfo), 1))
    }

    'MultipleSeparateElements {
      val buildInfo1 = BuildInfo.successful(Instant.ofEpochMilli(1), Instant.ofEpochMilli(2), BuildNumber(2))
      val buildInfo2 = BuildInfo.successful(Instant.ofEpochMilli(3), Instant.ofEpochMilli(4), BuildNumber(4))
      overlappingIslands(Traversable(buildInfo1, buildInfo2)) ==> Set(Island(Seq(buildInfo1), 1), Island(Seq(buildInfo2), 1))
    }

    'TwoOverlappingElement {
      val buildInfo1 = BuildInfo.successful(Instant.ofEpochMilli(1), Instant.ofEpochMilli(10), BuildNumber(2))
      val buildInfo2 = BuildInfo.successful(Instant.ofEpochMilli(3), Instant.ofEpochMilli(5), BuildNumber(15))
      overlappingIslands(Traversable(buildInfo1, buildInfo2)) ==> Set(Island(Seq(buildInfo1, buildInfo2), 2))
    }
  }

}
