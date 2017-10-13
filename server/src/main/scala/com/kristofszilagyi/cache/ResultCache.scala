package com.kristofszilagyi.cache

import akka.typed.scaladsl.Actor
import akka.typed.{ActorRef, Behavior}
import com.kristofszilagyi.fetchers.JenkinsFetcher.FetcherIncoming
import com.kristofszilagyi.shared.BulkFetchResult

sealed trait ResultCacheIncoming
object FetchCached extends ResultCacheIncoming
final case class FetchFinished(bulkFetchResult: BulkFetchResult) extends ResultCacheIncoming

final case class CachedResult(result: Option[BulkFetchResult])


class ResultCache {
  def behaviour(parent: ActorRef[CachedResult],
                fetcherBehaviour: Behavior[FetcherIncoming]): Behavior[ResultCacheIncoming] = {
    Actor.deferred { ctx =>
      val fetcher = ctx.spawn(fetcherBehaviour, "fetcher")
      ctx.watch(fetcher)
      //todo add error handling, what if the future does not complete or fail
      def b(cache: CachedResult): Behavior[ResultCacheIncoming] = {
        Actor.immutable[ResultCacheIncoming] { (_, msg) =>
          msg match {
            case FetchCached =>
              parent ! cache
              Actor.same
            case FetchFinished(result) =>
              b(CachedResult(Some(result)))
          }
        }
      }
      b(CachedResult(None))
    }
  }
}
