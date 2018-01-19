package com.kristofszilagyi.pipelineboard

import java.time.Instant

import com.kristofszilagyi.pipelineboard.ParallelJobManager.overlappingIslands
import com.kristofszilagyi.pipelineboard.shared.{BuildInfo, BuildNumber}
import utest._

object ParallelJobManagerTest  extends TestSuite {
  private def buildInfo(startMillis: Long, endMilli: Long): BuildInfo = {
    BuildInfo.successful(Instant.ofEpochMilli(startMillis), Instant.ofEpochMilli(endMilli), BuildNumber(1))
  }

  def tests = TestSuite {
    'OneElement {
      val b1 = buildInfo(1, 2)
      overlappingIslands(Traversable(b1)) ==> Set(Island(Seq(b1), 1))
    }

    'MultipleSeparateElements {
      val b1 = buildInfo(1, 2)
      val b2 = buildInfo(3, 4)
      overlappingIslands(Traversable(b1, b2)) ==> Set(Island(Seq(b1), 1), Island(Seq(b2), 1))
    }

    'TwoOverlappingElement {
      val b1 = buildInfo(1, 10)
      val b2 = buildInfo(3, 5)
      overlappingIslands(Traversable(b1, b2)) ==> Set(Island(Seq(b1, b2), 2))
    }

    'TwoOverlappingElementAndOneNot {
      val b1 = buildInfo(1, 10)
      val b2 = buildInfo(3, 5)
      val b3 = buildInfo(100, 200)
      overlappingIslands(Traversable(b1, b2, b3)) ==> Set(Island(Seq(b1, b2), 2), Island(Seq(b3), 1))
    }

    'ThreeOverlappingElementWithMax2 {
      val b1 = buildInfo(1, 10)
      val b2 = buildInfo(5, 20)
      val b3 = buildInfo(15, 25)
      overlappingIslands(Traversable(b1, b2, b3)) ==> Set(Island(Seq(b1, b2, b3), 2))
    }

    'ThreeOverlappingElementWithMax2WrongOrder {
      val b1 = buildInfo(1, 10)
      val b2 = buildInfo(5, 20)
      val b3 = buildInfo(15, 25)
      overlappingIslands(Traversable(b3, b2, b1)) ==> Set(Island(Seq(b1, b2, b3), 2))
    }

    'ThreeOverlappingElementWithMax3 {
      val b1 = buildInfo(1, 10)
      val b2 = buildInfo(5, 20)
      val b3 = buildInfo(6, 25)
      overlappingIslands(Traversable(b1, b2, b3)) ==> Set(Island(Seq(b1, b2, b3), 3))
    }


  }

}
