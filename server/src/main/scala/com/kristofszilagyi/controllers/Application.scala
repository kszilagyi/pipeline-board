package com.kristofszilagyi.controllers

import java.nio.file.{Files, Paths}
import javax.inject._

import akka.actor.Scheduler
import akka.typed.ActorSystem
import akka.typed.scaladsl.AskPattern._
import akka.util.Timeout
import com.kristofszilagyi.controllers.AutowireServer.throwEither
import com.kristofszilagyi.fetchers.JenkinsFetcher
import com.kristofszilagyi.fetchers.JenkinsFetcher.Fetch
import com.kristofszilagyi.shared._
import com.netaporter.uri.Uri
import io.circe.parser.decode
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder}
import play.api.Configuration
import play.api.mvc._

import scala.collection.JavaConverters._
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

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
    Ok(views.html.index("Pipeline monitor")(config))
  }


  @SuppressWarnings(Array(Wart.Throw))
  val autowireServer = {

    //todo fix for other OS
    //todo rename with project rename
    val home = System.getenv("HOME")
    val config = s"$home/.pipeline_monitor/config"
    val jobs = Files.readAllLines(Paths.get(config)).asScala.flatMap { line =>
      line.split(";").map(_.trim).toList match {
        case name :: url :: Nil =>
          Some(Job(JobName(name), JobUrl(Uri.parse(url)))).toList
        case Nil => None.toList   //skip empty lines
        case List("") => None.toList //skip empty lines
        case sgElse =>
          throw new RuntimeException(s"config in $config has wrong line: $sgElse, tpe: ${sgElse.getClass}")
      }
    }
    new AutowireServer(new AutowireApiImpl(fetcher, jobs))
  }

  def autowireApi(path: String): Action[AnyContent] = Action.async { implicit request =>

    // call Autowire route
    autowireServer.routes(
      autowire.Core.Request(path.split("/"), request.queryString.mapValues(_.mkString))
    ).map(s => {
      Ok(s)
    })
  }

}
