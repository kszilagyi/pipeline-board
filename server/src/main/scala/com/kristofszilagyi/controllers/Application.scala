package com.kristofszilagyi.controllers

import javax.inject._

import akka.actor.Scheduler
import akka.typed.scaladsl.AskPattern._
import akka.typed.ActorSystem
import akka.util.{ByteString, Timeout}
import com.kristofszilagyi.controllers.AutowireServer.throwEither
import com.kristofszilagyi.fetchers.JenkinsFetcher.Fetch
import com.kristofszilagyi.fetchers.{JenkinsFetcher, JenkinsJobUrl}
import com.kristofszilagyi.shared.{AutowireApi, BulkFetchResult, JobDetails, Wart}
import com.netaporter.uri.Uri
import io.circe.{Decoder, Encoder}
import play.api.Configuration
import play.api.mvc._
import io.circe.syntax.EncoderOps
import io.circe.parser.decode

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.DurationInt

class AutowireApiImpl(fetcher: JenkinsFetcher) extends AutowireApi {
  def dataFeed(): Future[BulkFetchResult] = {
    implicit val timeout: Timeout = Timeout(10.seconds)
    val system: ActorSystem[Fetch] = ActorSystem("Demo", fetcher.behaviour)
    implicit val scheduler: Scheduler = system.scheduler
    system ? { Fetch(
      List(JenkinsJobUrl(Uri.parse("http://localhost:8080/job/Other%20stuff")),
        JenkinsJobUrl(Uri.parse("http://localhost:8080/job/One%20stuff")),
        JenkinsJobUrl(Uri.parse("http://localhost:8080/job/Slow%20stuff")),
      ),
      _
      )}
  }
}

object AutowireServer {
  //mandated by the autowire api :(
  @SuppressWarnings(Array(Wart.EitherProjectionPartial, Wart.Throw))
  def throwEither[A](either: Either[io.circe.Error, A]): A = {
    either.right.getOrElse(throw either.left.get)
  }
}

class AutowireServer(impl: AutowireApiImpl)(implicit ec: ExecutionContext) extends autowire.Server[String, Decoder, Encoder]{
  def write[R: Encoder](r: R): String = r.asJson.spaces2
  def read[R: Decoder](p: String): R = {
    val either = decode[R](p)
    throwEither(either)
  }

  val routes: Router = route[AutowireApi](impl)
}


class Application @Inject() (fetcher: JenkinsFetcher)(val config: Configuration)
                            (implicit ec: ExecutionContext) extends InjectedController {

  def root: Action[AnyContent] = Action {
    Ok(views.html.index("Multi CI dashboard")(config))
  }

  val autowireServer = new AutowireServer(new AutowireApiImpl(fetcher))

  def autowireApi(path: String): Action[AnyContent] = Action.async { implicit request =>

    // call Autowire route
    autowireServer.routes(
      autowire.Core.Request(path.split("/"), request.queryString.mapValues(_.mkString))
    ).map(s => {
      Ok(s)
    })
  }

}
