package com.kristofszilagyi.controllers

import akka.actor.Scheduler
import akka.typed.ActorSystem
import akka.util.Timeout
import com.kristofszilagyi.fetchers.JenkinsFetcher
import com.kristofszilagyi.fetchers.JenkinsFetcher.Fetch
import com.kristofszilagyi.shared.{AutowireApi, BulkFetchResult, Job}
import scala.concurrent.duration.DurationInt
import akka.typed.scaladsl.AskPattern._

import scala.concurrent.Future

class AutowireApiImpl(fetcher: JenkinsFetcher, jobs: Seq[Job]) extends AutowireApi {
  implicit val timeout: Timeout = Timeout(10.seconds)
  val system: ActorSystem[Fetch] = ActorSystem("Demo", fetcher.behaviour)
  implicit val scheduler: Scheduler = system.scheduler

  def dataFeed(): Future[BulkFetchResult] = {
    system ? { Fetch(
      jobs,
      _//todo old jobs do not show up on the rest API (just the 100 newest)
    )}
  }
}