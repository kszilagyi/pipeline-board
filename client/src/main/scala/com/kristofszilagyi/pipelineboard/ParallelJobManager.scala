package com.kristofszilagyi.pipelineboard

import com.kristofszilagyi.pipelineboard.shared.BuildInfo
import slogging.LazyLogging
import com.kristofszilagyi.pipelineboard.shared.TypeSafeEqualsOps._
import scala.annotation.tailrec

object Island {
  def empty: Island = Island(List.empty)
  def of(b: BuildInfo): Island = {
    Island(List(b))
  }
}


final case class Island(builds: List[BuildInfo]) {
  def add(newBuild: BuildInfo): Island = {
    Island(builds :+ newBuild)
  }

  def isEmpty: Boolean = builds.isEmpty

}

/**
  * 0-based
  */
final case class Slot(i: Int)


final case class SlottedIsland(builds: Map[Slot, List[BuildInfo]])

object ParallelJobManager extends LazyLogging {
  def overlappingIslands(builds: Traversable[BuildInfo]): Traversable[Island] = {
    val sortedBuilds = builds.toSeq.sortBy(_.buildStart)
    rec(sortedBuilds, Island.empty, Seq.empty)
  }

  @tailrec
  private def rec(remainingSortedBuilds: Seq[BuildInfo], ongoingIsland: Island, finishedIslands: Seq[Island]): Traversable[Island] = {
    remainingSortedBuilds.toList match {
      case head :: tail =>
        val overlaps = ongoingIsland.builds.filter(_.overlap(head))
        if (overlaps.nonEmpty) {
          rec(tail, ongoingIsland.add(head), finishedIslands)
        } else {
          val newFinishedIslands = if (ongoingIsland.isEmpty) finishedIslands
                         else finishedIslands :+ ongoingIsland
          rec(tail, Island.of(head), newFinishedIslands)
        }
      case Nil =>
        finishedIslands :+ ongoingIsland
    }
  }

  @tailrec
  private def recSlotify(remainingBuilds: List[BuildInfo], slots: Map[Slot, List[BuildInfo]]): SlottedIsland = {
    remainingBuilds match {
      case build :: rest =>
        val sortedSlots = slots.toSeq.sortBy(_._1.i)
        val maybeFreeSlot = sortedSlots.find{ case (slot, buildInfos) =>
          buildInfos.forall(_.overlap(build) ==== false)
        }
        val (freeSlot, buildsInSlot) = maybeFreeSlot match {
          case Some(slot) =>
            slot
          case None =>
            Slot(sortedSlots.size) -> List.empty
        }
        recSlotify(rest, slots + (freeSlot -> (buildsInSlot :+ build)))
      case Nil =>
        SlottedIsland(slots)
    }

  }


  def slotify(islands: Traversable[Island]): Traversable[SlottedIsland] = {
    islands.map{ island =>
      recSlotify(island.builds, Map.empty)
    }
  }
}
