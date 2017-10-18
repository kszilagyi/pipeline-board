package com.kristofszilagyi.controllers

import java.nio.file.{Files, Paths}
import javax.inject._

import com.kristofszilagyi.Aggregator
import com.kristofszilagyi.cache.ResultCache
import com.kristofszilagyi.fetchers.JenkinsFetcher
import com.kristofszilagyi.shared._
import com.netaporter.uri.Uri
import play.api.Configuration
import play.api.libs.ws.WSClient
import play.api.mvc._

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import CssSettings.settings._


class Application @Inject() (wsClient: WSClient)(val config: Configuration)
                                             (implicit ec: ExecutionContext) extends InjectedController {

  def root: Action[AnyContent] = Action {
    Ok(views.html.index("Pipeline monitor")(config))
  }

  def css: Action[AnyContent] = Action {
    Ok(MyStyles.render).as(CSS)
  }


  @SuppressWarnings(Array(Wart.Throw))
  private val autowireServer = {
    //todo fix for other OS
    //todo rename with project rename
    val home = System.getenv("HOME")
    val config = s"$home/.pipeline_monitor/config"
    val jobs = Files.readAllLines(Paths.get(config)).asScala.flatMap { line =>
      line.split(";").map(_.trim).toList match {
        case name :: url :: jobType :: Nil =>
          Some(Job(JobName(name), JobUrl(Uri.parse(url)), JobType.withNameInsensitive(jobType))).toList
        case Nil => None.toList   //skip empty lines
        case List("") => None.toList //skip empty lines
        case sgElse =>
          throw new RuntimeException(s"config in $config has wrong line: $sgElse, tpe: ${sgElse.getClass}")
      }
    }
    val aggregator = new Aggregator(Seq(new JenkinsFetcher(wsClient, "jenkins", jobs)))
    val resultCache = new ResultCache(aggregator.behavior)
    new AutowireServer(new AutowireApiImpl(resultCache))
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
