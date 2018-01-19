package com.kristofszilagyi.pipelineboard

import com.kristofszilagyi.pipelineboard.shared.BuildInfo

import scala.annotation.tailrec

object Island {
  def empty: Island = Island(Seq.empty, 0)
  def of(b: BuildInfo): Island = {
    Island(Seq(b), 1)
  }
}

/**
  * @param maxOverlap max number of elements overlap in this island. No overlap => 1
  */
final case class Island(builds: Seq[BuildInfo], maxOverlap: Int) {
  def add(newBuild: BuildInfo, overlaps: Int): Island = {
    Island(builds :+ newBuild, maxOverlap.max(overlaps))
  }

  def isEmpty: Boolean = builds.isEmpty

}

object ParallelJobManager {
  def overlappingIslands(builds: Traversable[BuildInfo]): Set[Island] = {
    val sortedBuilds = builds.toSeq.sortBy(_.buildStart)
    rec(sortedBuilds, Island.empty, Set.empty)
  }

  @tailrec
  private def rec(remainingSortedBuilds: Seq[BuildInfo], ongoingIsland: Island, finishedIslands: Set[Island]): Set[Island] = {
    remainingSortedBuilds match {
      case head :: tail =>
        val overlaps = ongoingIsland.builds.filter(_.overlap(head))
        if (overlaps.nonEmpty) {
          rec(tail, ongoingIsland.add(head, overlaps.size + 1), finishedIslands)
        } else {
          val newFinishedIslands = if (ongoingIsland.isEmpty) finishedIslands
                         else finishedIslands + ongoingIsland
          rec(tail, Island.of(head), newFinishedIslands)
        }
      case Nil =>
        finishedIslands + ongoingIsland
    }
  }
}
