package com.kristofszilagyi

sealed trait BidirectionalIterator[A] {
  def idx: Int
  def value: A
  def moveLeft: BidirectionalIterator[A]
  def moveRight: BidirectionalIterator[A]
}

object BidirectionalIterator {

  def apply[A](s: Seq[A], idx: Int): BidirectionalIterator[A] = {
    val actualIdx: Int = math.max(0, math.min(s.size - 1, idx))

    new UnsafeBidirectionalIterator[A](s, actualIdx)
  }
  /**
    * This doesn't overflow but uses the higest or lowest value
    */
  private final case class UnsafeBidirectionalIterator[A](s: Seq[A], idx: Int) extends BidirectionalIterator[A] {

    def value: A = s(idx)

    override def moveLeft: BidirectionalIterator[A] = BidirectionalIterator(s, idx - 1)

    override def moveRight: BidirectionalIterator[A] = BidirectionalIterator(s, idx + 1)
  }

}


object SeqOps {
  implicit class RichSeq[A](s: Seq[A]) {
    def biIterator(i: Int): BidirectionalIterator[A] = {
      BidirectionalIterator(s, i)
    }
  }
}