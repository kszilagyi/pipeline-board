package com.kristofszilagyi.controllers

import java.time.Instant

import akka.actor.Scheduler
import akka.typed.ActorSystem
import akka.typed.scaladsl.AskPattern._
import akka.util.Timeout
import com.kristofszilagyi.cache.{FetchCached, ResultCache}
import com.kristofszilagyi.shared.{AutowireApi, ResultAndTime}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

class AutowireApiImpl(resultCache: ResultCache) extends AutowireApi {
  private implicit val timeout: Timeout = Timeout(10.seconds)
  private val system = ActorSystem(resultCache.behaviour, "pipeline-monitor")
  private implicit val scheduler: Scheduler = system.scheduler
  private implicit val ec: ExecutionContext = system.executionContext

  def dataFeed(): Future[ResultAndTime] = {
    (system ? { FetchCached(
      _
    )}).map(ResultAndTime(_, Instant.now))
    //todo old jobs do not show up on the rest API (just the 100 newest)
  }
}