package com.kristofszilagyi.cache

import akka.typed.scaladsl.Actor
import akka.typed.{ActorRef, Behavior}
import com.kristofszilagyi.fetchers.{Fetcher, GitLabCiFetcher, JenkinsFetcher}
import com.kristofszilagyi.shared.JobType.{GitLabCi, Jenkins}
import com.kristofszilagyi.shared.TypeSafeEqualsOps._
import com.kristofszilagyi.shared._
import play.api.libs.ws.WSClient
import slogging.LazyLogging

import scala.collection.immutable.ListSet

sealed trait ResultCacheIncoming
final case class FetchCached(parent: ActorRef[CachedResult]) extends ResultCacheIncoming
final case class OneFetchFinished(result: JobDetails) extends ResultCacheIncoming

class ResultCache(jobs: ListSet[Job], fetchers: Traversable[Fetcher]) extends LazyLogging {

  val behaviour: Behavior[ResultCacheIncoming] = {
    Actor.deferred { ctx =>
      fetchers.foreach{ fetcher =>
        val ref = ctx.spawn(fetcher.behaviour, fetcher.name)
        ctx.watch(ref)
      }

      @SuppressWarnings(Array(Wart.Recursion))
      def b(cache: CachedResult): Behavior[ResultCacheIncoming] = {
        Actor.immutable[ResultCacheIncoming] { (_, msg) =>
          msg match {
            case FetchCached(sender) =>
              sender ! cache
              Actor.same
            case newResult: OneFetchFinished =>
              val matchingJobs = cache.results.filter(_.static ==== newResult.result.static).toList
              matchingJobs match {
                case List() =>
                  logger.error(s"No matching jobs for ${newResult.result.static}")
                  Actor.stopped
                case List(jobToUpdate) =>
                  val newCache = cache.results.updated(cache.results.indexOf(jobToUpdate), newResult.result)
                  b(CachedResult(newCache))
                case other =>
                  logger.error(s"More than one matching jobs for ${newResult.result.static}. matching = $other")
                  Actor.stopped
              }
          }
        }
      }
      b(CachedResult(jobs.toSeq.map(JobDetails(_, None))))
    }
  }
}
