package com.kristofszilagyi.pipelineboard

import java.time.Instant

import com.kristofszilagyi.pipelineboard.ParallelJobManager.{overlappingIslands, slotify}
import com.kristofszilagyi.pipelineboard.shared.{BuildInfo, BuildNumber}
import utest._

object ParallelJobManagerTest  extends TestSuite {
  private def buildInfo(startMillis: Long, endMilli: Long): BuildInfo = {
    BuildInfo.successful(Instant.ofEpochMilli(startMillis), Instant.ofEpochMilli(endMilli), BuildNumber(1))
  }

  def tests = TestSuite {
    'OneElement {
      val b1 = buildInfo(1, 2)
      overlappingIslands(Traversable(b1)) ==> Set(Island(List(b1)))
    }

    'MultipleSeparateElements {
      val b1 = buildInfo(1, 2)
      val b2 = buildInfo(3, 4)
      overlappingIslands(Traversable(b1, b2)) ==> Set(Island(List(b1)), Island(List(b2)))
    }

    'TwoOverlappingElement {
      val b1 = buildInfo(1, 10)
      val b2 = buildInfo(3, 5)
      overlappingIslands(Traversable(b1, b2)) ==> Set(Island(List(b1, b2)))
    }

    'TwoOverlappingElementAndOneNot {
      val b1 = buildInfo(1, 10)
      val b2 = buildInfo(3, 5)
      val b3 = buildInfo(100, 200)
      overlappingIslands(Traversable(b1, b2, b3)) ==> Set(Island(List(b1, b2)), Island(List(b3)))
    }

    'ThreeOverlappingElementWithMax2 {
      val b1 = buildInfo(1, 10)
      val b2 = buildInfo(5, 20)
      val b3 = buildInfo(15, 25)
      overlappingIslands(Traversable(b1, b2, b3)) ==> Set(Island(List(b1, b2, b3)))
    }

    'ThreeOverlappingElementWithMax2WrongOrder {
      val b1 = buildInfo(1, 10)
      val b2 = buildInfo(5, 20)
      val b3 = buildInfo(15, 25)
      overlappingIslands(Traversable(b3, b2, b1)) ==> Set(Island(List(b1, b2, b3)))
    }

    'ThreeOverlappingElementWithMax3 {
      val b1 = buildInfo(1, 10)
      val b2 = buildInfo(5, 20)
      val b3 = buildInfo(6, 25)
      overlappingIslands(Traversable(b1, b2, b3)) ==> Set(Island(List(b1, b2, b3)))
    }

    'SlotifiyingOne {
      val b1 = buildInfo(1, 10)
      slotify(Set(Island(List(b1)))) ==> Set(SlottedIsland(Map(Slot(0) -> List(b1))))
    }

    'SlotifiyingTwoIndependent {
      val b1 = buildInfo(1, 10)
      val b2 = buildInfo(11, 20)
      slotify(Set(Island(List(b1)), Island(List(b2)))) ==>
        Set(
          SlottedIsland(Map(Slot(0) -> List(b1))),
          SlottedIsland(Map(Slot(0) -> List(b2)))
      )
    }

    'SlotifyingTwoInTheSameIsland {
      val b1 = buildInfo(1, 10)
      val b2 = buildInfo(5, 20)
      slotify(Set(Island(List(b1, b2)))) ==>
        Set(
          SlottedIsland(Map(
            Slot(0) -> List(b1),
            Slot(1) -> List(b2)
          ))
        )
    }

    'SlotifyingThreeInTheSameIsland3OverLapping {
      val b1 = buildInfo(1, 10)
      val b2 = buildInfo(5, 20)
      val b3 = buildInfo(6, 21)
      slotify(Set(Island(List(b1, b2, b3)))) ==>
        Set(
          SlottedIsland(Map(
            Slot(0) -> List(b1),
            Slot(1) -> List(b2),
            Slot(2) -> List(b3)
          ))
        )
    }

    'SlotifyingThreeInTheSameIsland2OverLapping {
      val b1 = buildInfo(1, 10)
      val b2 = buildInfo(5, 20)
      val b3 = buildInfo(11, 21)
      slotify(Set(Island(List(b1, b2, b3)))) ==>
        Set(
          SlottedIsland(Map(
            Slot(0) -> List(b1, b3),
            Slot(1) -> List(b2)
          ))
        )
    }

  }

}
