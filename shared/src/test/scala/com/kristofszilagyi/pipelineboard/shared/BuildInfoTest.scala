package com.kristofszilagyi.pipelineboard.shared

import java.time.Instant

import utest._
import utest.framework.{Test, Tree}

object BuildInfoTest extends TestSuite {

  private def buildInfo(startMillis: Long, endMilli: Option[Long]): BuildInfo = {
    BuildInfo(BuildStatus.Successful, Instant.ofEpochMilli(startMillis), endMilli.map(Instant.ofEpochMilli), BuildNumber(1))
  }
  
  def tests: Tree[Test] = this {
    'overlapFirstLeft {
      val b1 = buildInfo(10, Some(20))
      val b2 = buildInfo(15, Some(25))
      b1.overlap(b2) ==> true
    }

    'overlapFirstRight {
      val b1 = buildInfo(10, Some(20))
      val b2 = buildInfo(15, Some(25))
      b2.overlap(b1) ==> true
    }

    'overlapSame {
      val b1 = buildInfo(10, Some(20))
      val b2 = buildInfo(10, Some(20))
      b2.overlap(b1) ==> true
    }

    'noOverLapFirstRight {
      val b1 = buildInfo(10, Some(20))
      val b2 = buildInfo(25, Some(30))
      b1.overlap(b2) ==> false
    }

    'noOverLapFirstLeft {
      val b1 = buildInfo(10, Some(20))
      val b2 = buildInfo(25, Some(30))
      b2.overlap(b1) ==> false
    }
    
    'nonOverlappingInfiniteRightFirst {
      val b1 = buildInfo(10, None)
      val b2 = buildInfo(1, Some(5))
      b1.overlap(b2) ==> false
    }

    'nonOverlappingInfiniteRightSecond {
      val b1 = buildInfo(10, None)
      val b2 = buildInfo(1, Some(5))
      b2.overlap(b1) ==> false
    }

    'overlappingInfiniteLeftFirst {
      val b1 = buildInfo(1, None)
      val b2 = buildInfo(10, Some(15))
      b1.overlap(b2) ==> true
    }

    'overlappingInfiniteLeftSecond {
      val b1 = buildInfo(1, None)
      val b2 = buildInfo(10, Some(15))
      b2.overlap(b1) ==> true
    }

    'overlappingInfiniteRightFirst {
      val b1 = buildInfo(10, None)
      val b2 = buildInfo(5, Some(15))
      b1.overlap(b2) ==> true
    }

    'overlappingInfiniteRightSecond {
      val b1 = buildInfo(10, None)
      val b2 = buildInfo(5, Some(15))
      b2.overlap(b1) ==> true
    }

    'overlappingFiniteInsideInfiniteFirst {
      val b1 = buildInfo(10, None)
      val b2 = buildInfo(11, Some(15))
      b1.overlap(b2) ==> true
    }

    'overlappingFiniteInsideInfiniteSecond {
      val b1 = buildInfo(10, None)
      val b2 = buildInfo(11, Some(15))
      b2.overlap(b1) ==> true
    }

    'overlappingFiniteInsideInfiniteFirstEdge {
      val b1 = buildInfo(10, None)
      val b2 = buildInfo(10, Some(15))
      b1.overlap(b2) ==> true
    }

    'overlappingFiniteInsideInfiniteSecondEdge {
      val b1 = buildInfo(10, None)
      val b2 = buildInfo(10, Some(15))
      b2.overlap(b1) ==> true
    }
  }
}
