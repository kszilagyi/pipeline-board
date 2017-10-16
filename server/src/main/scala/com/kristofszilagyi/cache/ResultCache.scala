package com.kristofszilagyi.cache

import akka.typed.scaladsl.Actor
import akka.typed.{ActorRef, Behavior}
import com.kristofszilagyi.FetchAll
import com.kristofszilagyi.shared.Wart._
import com.kristofszilagyi.shared.{AllResult, CachedResult, Wart}

import scala.annotation.tailrec
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationLong

sealed trait ResultCacheIncoming
final case class FetchCached(parent: ActorRef[CachedResult]) extends ResultCacheIncoming
final case class AllFetchFinished(allResult: AllResult) extends ResultCacheIncoming

class ResultCache(aggregator: Behavior[FetchAll]) {

  val behaviour: Behavior[ResultCacheIncoming] = {
    Actor.deferred { ctx =>
      val aggregatorRef = ctx.spawn(aggregator, "aggregator")
      ctx.watch(aggregatorRef)

      implicit val ec: ExecutionContext = ctx.executionContext
      discard(ctx.system.scheduler.schedule(0.seconds, 10.seconds) {
        aggregatorRef ! FetchAll(ctx.self)
      })
      @SuppressWarnings(Array(Wart.Recursion))
      def b(cache: CachedResult): Behavior[ResultCacheIncoming] = {
        Actor.immutable[ResultCacheIncoming] { (_, msg) =>
          msg match {
            case FetchCached(sender) =>
              sender ! cache
              Actor.same
            case finished: AllFetchFinished =>
              b(CachedResult(Some(finished.allResult)))
          }
        }
      }
      b(CachedResult(None))
    }
  }
}
