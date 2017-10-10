package com.kristofszilagyi.controllers

import java.nio.file.{Files, Paths}
import javax.inject._

import com.kristofszilagyi.fetchers.JenkinsFetcher
import com.kristofszilagyi.shared._
import com.netaporter.uri.Uri
import play.api.Configuration
import play.api.mvc._

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext


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
