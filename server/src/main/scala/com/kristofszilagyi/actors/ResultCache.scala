package com.kristofszilagyi.actors

import akka.typed.scaladsl.Actor
import akka.typed.{ActorRef, Behavior}
import com.kristofszilagyi.fetchers.Fetcher
import com.kristofszilagyi.shared.TypeSafeEqualsOps._
import com.kristofszilagyi.shared._
import slogging.LazyLogging

import scala.collection.immutable.ListMap

sealed trait ResultCacheIncoming
final case class FetchCached(parent: ActorRef[CachedResult]) extends ResultCacheIncoming
final case class OneFetchFinished(result: JobDetails) extends ResultCacheIncoming

final class ResultCache(jobGroups: ListMap[GroupName, Seq[Job]], fetchers: Traversable[Fetcher]) extends LazyLogging {
  val behaviour: Behavior[ResultCacheIncoming] = {
    Actor.deferred { ctx =>
      fetchers.foreach{ fetcher =>
        val fetcherRef = ctx.spawn(fetcher.behaviour, fetcher.name)
        ctx.watch(fetcherRef)
        val heartbeatRef = ctx.spawn(new Heartbeat(fetcherRef, ctx.self).behaviour, fetcher.name + "-heart")
        ctx.watch(heartbeatRef)
      }

      @SuppressWarnings(Array(Wart.Recursion))
      def b(cache: CachedResult): Behavior[ResultCacheIncoming] = {
        Actor.immutable[ResultCacheIncoming] { (_, msg) =>
          msg match {
            case FetchCached(sender) =>
              sender ! cache
              Actor.same
            case newResult: OneFetchFinished =>
              val newCache = cache.groups.map {case (name, group) => {
                name -> JobGroup(group.jobs.map{jobDetails =>
                  if (jobDetails.static ==== newResult.result.static) {
                    newResult.result
                  } else {
                    jobDetails
                  }
                })
              }}
              b(CachedResult(newCache))
          }
        }
      }

      val initialCache = jobGroups.map{case (groupName, jobs) =>
        groupName -> JobGroup(jobs.map(JobDetails(_, None)))
      }
      b(CachedResult(initialCache.toSeq))
    }
  }
}
