package com.kristofszilagyi.pipelineboard

import utest._

object BidirectionalIteratorTest extends TestSuite {
  private val testSeq = List(0, 10, 20, 30, 40, 50, 60, 70)

  def tests = TestSuite {
    'CanCreateOnFirstElement {
      BidirectionalIterator(testSeq, 0).value ==> 0
    }

    'CanCreateOnSecondElement {
      BidirectionalIterator(testSeq, 2).value ==> 20
    }

    'CanCreateOnLastElement {
      BidirectionalIterator(testSeq, 6).value ==> 60
    }

    'CanCreateOnLastMinus1Element {
      BidirectionalIterator(testSeq, 7).value ==> 70
    }

    'OverflowIsNormalizedMin {
      BidirectionalIterator(testSeq, -1).value ==> 0
    }

    'OverflowIsNormalizedMax {
      BidirectionalIterator(testSeq, 8).value ==> 70
    }

    'MoveLeftNormal {
      BidirectionalIterator(testSeq, 2).moveLeft.value ==> 10
    }

    'MoveRightNormal {
      BidirectionalIterator(testSeq, 2).moveRight.value ==> 30
    }

    'MoveLeftOverFlow {
      BidirectionalIterator(testSeq, 0).moveLeft.value ==> 0
    }

    'MoveRightOverFlow {
      BidirectionalIterator(testSeq, 7).moveRight.value ==> 70
    }


  }
}
