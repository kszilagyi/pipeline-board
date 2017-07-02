package com.kristofszilagyi.controllers

import javax.inject._

import akka.actor.Scheduler
import akka.typed.scaladsl.AskPattern._
import akka.typed.ActorSystem
import akka.util.Timeout
import com.kristofszilagyi.fetchers.JenkinsFetcher.{Fetch, FetchResult}
import com.kristofszilagyi.fetchers.{JenkinsFetcher, JenkinsJobUrl}
import com.netaporter.uri.Uri
import play.api.Configuration
import play.api.mvc._

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class Application @Inject() (fetcher: JenkinsFetcher)(val config: Configuration) extends InjectedController {

  def index: Action[AnyContent] = Action {
    implicit val timeout: Timeout = Timeout(1.seconds)
    val system: ActorSystem[Fetch] = ActorSystem("Demo", fetcher.behaviour)
    implicit val scheduler: Scheduler = system.scheduler
    val x: Future[FetchResult] = system ? (Fetch(JenkinsJobUrl(Uri.parse("http://localhost:8080/job/Other%20stuff")), _))
    println(Await.result(x, 10.seconds))
    Ok(views.html.index("Multi CI Dashboard")(config))
  }

}
