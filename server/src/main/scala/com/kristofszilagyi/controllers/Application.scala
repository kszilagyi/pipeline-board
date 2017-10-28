package com.kristofszilagyi.controllers

import javax.inject._

import com.kristofszilagyi.Aggregator
import com.kristofszilagyi.cache.ResultCache
import com.kristofszilagyi.fetchers.{GitLabCiFetcher, GitLabCiJob, JenkinsFetcher}
import com.kristofszilagyi.shared.CssSettings.settings._
import com.kristofszilagyi.shared.JobType.{GitLabCi, Jenkins}
import com.kristofszilagyi.shared._
import net.jcazevedo.moultingyaml.PimpedString
import play.api.Configuration
import play.api.libs.ws.WSClient
import play.api.mvc._
import com.netaporter.uri.dsl._

import scala.concurrent.ExecutionContext
import scala.io.Source


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
    val configPath = s"$home/.pipeline_monitor/config"
    val config = Config.format.read(Source.fromFile(configPath).mkString.parseYaml)
    val jenkinsJobs = config.jenkins.jobs.map(jobConfig =>
      Job(jobConfig.name, Urls(userRoot = jobConfig.url,restRoot = RestRoot(jobConfig.url.u / "api/json")), Jenkins)
    )
    val gitLabJobs = config.gitLab.jobs.map(jobConfig =>
      GitLabCiJob(
        Job(jobConfig.name, Urls(userRoot = jobConfig.userUrl, restRoot = jobConfig.restRoot), GitLabCi),
        jobConfig.accessToken, jobConfig.jobNameOnGitLab
      )
    )
    val aggregator = new Aggregator(Seq(
      new JenkinsFetcher(wsClient, jenkinsJobs),
      new GitLabCiFetcher(wsClient, gitLabJobs)
    ))
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
