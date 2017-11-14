package com.kristofszilagyi

import java.time.Instant

import akka.actor.Scheduler
import akka.typed.scaladsl.Actor
import akka.typed.scaladsl.AskPattern._
import akka.typed.{ActorRef, Behavior}
import akka.util.Timeout
import com.kristofszilagyi.cache.AllFetchFinished
import com.kristofszilagyi.fetchers.Fetcher
import com.kristofszilagyi.fetchers.JenkinsFetcher.Fetch
import com.kristofszilagyi.shared.{AllResult, FetcherResult}

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{ExecutionContext, Future}

sealed trait AggregatorIncoming
final case class FetchAll(sender: ActorRef[AllFetchFinished]) extends AggregatorIncoming
final case class FetchFinished(result: FetcherResult) extends AggregatorIncoming

class Aggregator(fetchers: Seq[Fetcher]) {
  val behavior: Behavior[FetchAll] = {
    Actor.deferred { ctx =>
      val fetcherRefs = fetchers.map{ fetcher =>
        val ref = ctx.spawn(fetcher.behaviour, fetcher.name)
        ctx.watch(ref)
        ref
      }
      //todo change this to be without timeout and ask
      implicit val timeout: Timeout = Timeout(30.seconds)
      implicit val scheduler: Scheduler = ctx.system.scheduler
      implicit val ec: ExecutionContext = ctx.executionContext

      Actor.immutable{ (innerCtx, msg) =>
        msg match {
          case FetchAll(sender) =>
            Future.sequence(fetcherRefs.map { fetcher =>
              fetcher ? Fetch
            }).foreach { results =>
              sender ! AllFetchFinished(AllResult(results.flatMap(_.results), Instant.now))
            }
            Actor.same
        }
      }
    }
  }
}
