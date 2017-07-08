package com.kristofszilagyi.controllers

import javax.inject._

import akka.actor.Scheduler
import akka.typed.scaladsl.AskPattern._
import akka.typed.ActorSystem
import akka.util.{ByteString, Timeout}
import com.kristofszilagyi.fetchers.JenkinsFetcher.Fetch
import com.kristofszilagyi.fetchers.{FetchResult, JenkinsFetcher, JenkinsJobUrl}
import com.netaporter.uri.Uri
import io.circe.Encoder
import play.api.Configuration
import play.api.mvc._
import io.circe.syntax.EncoderOps
import play.api.http.{ContentTypeOf, ContentTypes, Writeable}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

object CirceJson {
  implicit def writeable[T](implicit encoder: Encoder[T]): Writeable[T] = {
    Writeable(data => ByteString(encoder.apply(data).spaces2))
  }

  implicit def contentType[T]: ContentTypeOf[T] = {
    ContentTypeOf(Some(ContentTypes.JSON))
  }
}
class Application @Inject() (fetcher: JenkinsFetcher)(val config: Configuration)
                            (implicit ec: ExecutionContext) extends InjectedController {

  def root: Action[AnyContent] = Action {

    Ok(views.html.index("Multi CI dashboard")(config))
  }

  def dataFeed: Action[AnyContent] = Action.async {
    import CirceJson._
    implicit val timeout: Timeout = Timeout(1.seconds)
    val system: ActorSystem[Fetch] = ActorSystem("Demo", fetcher.behaviour)
    implicit val scheduler: Scheduler = system.scheduler
    val fetchResult: Future[FetchResult] = system ? (Fetch(JenkinsJobUrl(Uri.parse("http://localhost:8080/job/Other%20stuff")), _))
    fetchResult.map(c => Ok(c.asJson))
  }

}
