package com.kristofszilagyi.controllers

import java.net.URLEncoder
import javax.inject._

import com.kristofszilagyi.actors.ResultCache
import com.kristofszilagyi.fetchers._
import com.kristofszilagyi.shared.CssSettings.settings._
import com.kristofszilagyi.shared.JobType.{GitLabCi, Jenkins}
import com.kristofszilagyi.shared._
import com.netaporter.uri.{PathPart, Uri}
import net.jcazevedo.moultingyaml.PimpedString
import play.api.Configuration
import play.api.libs.ws.WSClient
import play.api.mvc._
import com.netaporter.uri.dsl._
import slogging.{LazyLogging, LoggerConfig, PrintLoggerFactory}
import TypeSafeEqualsOps._

import scala.collection.immutable.{ListMap, ListSet}
import scala.concurrent.ExecutionContext
import scala.io.Source

class Application @Inject() (wsClient: WSClient)(val config: Configuration)
                                             (implicit ec: ExecutionContext) extends InjectedController with LazyLogging{

  LoggerConfig.factory = PrintLoggerFactory()

  def root: Action[AnyContent] = Action {
    Ok(views.html.index("Pipeline board")(config))
  }

  def css: Action[AnyContent] = Action {
    Ok(MyStyles.render).as(CSS)
  }


  @SuppressWarnings(Array(Wart.OptionPartial))
  private val autowireServer = {
    //todo fix for other OS
    //todo rename with project rename
    val home = System.getenv("HOME")
    val configPath = s"$home/.pipeline_board/config"
    val config = Config.format.read(Source.fromFile(configPath).mkString.parseYaml)
    logger.info(s"Congif is: $config")

    @SuppressWarnings(Array(Wart.Throw))
    val groupedJobs = ListMap(config.groups.map { group =>
      val jenkinsJobs = group.jenkins.map(_.jobs).getOrElse(Seq.empty).map { jobConfig =>
        val creds = (jobConfig.user, jobConfig.accessToken) match {
          case (Some(user), Some(token)) => Some(JenkinsCredentials(user, token))
          case (None, None) => None
          case other => throw new AssertionError(s"Either both access token and user has to be defined or neither: $other")
        }
        JenkinsJob(
          Job(jobConfig.name, Urls(userRoot = jobConfig.url, restRoot = RestRoot(jobConfig.url.u / "api/json")), Jenkins),
          creds
        )
      }

      val gitLabJobs = group.gitLabCi.map(_.jobs).getOrElse(Seq.empty).map { jobConfig =>
        val root = jobConfig.url
        val jobPath = URLEncoder.encode(root.u.u.pathParts.map(_.part).mkString("/"), "utf-8")
        val restRoot = root.u.u.copy(pathParts = Seq(PathPart("api"), PathPart("v4"), PathPart("projects")) :+ PathPart(jobPath)) //todo url encode
        GitLabCiJob(
          Job(jobConfig.name, Urls(userRoot = root, restRoot = RestRoot(RawUrl(restRoot))), GitLabCi),
          jobConfig.accessToken, jobConfig.jobNameOnGitLab
        )
      }
      (group.groupName, (jenkinsJobs, gitLabJobs))
    }: _*)

    val jenkinsJobs = groupedJobs.toList.flatMap(_._2._1)
    val gitLabJobs = groupedJobs.toList.flatMap(_._2._2)
    val fetchers =
      jenkinsJobs.map(new JenkinsFetcher(wsClient, _)) ++ gitLabJobs.map(new GitLabCiFetcher(wsClient, _))

    val jobs = jenkinsJobs.map(_.common) ++ gitLabJobs.map(_.common)
    val jobSet = ListSet(jobs: _*)
    assert(jobs.size ==== jobSet.size, "Jobs are not unique")
    assert(jobs.map(_.name).toSet.size ==== jobs.map(_.name).size, "Jobs names are not unique")
    val jobsForCache = groupedJobs.map{case (name, (jenkins, gitlab)) =>
        name -> (jenkins.map(_.common) ++ gitlab.map(_.common))
    }
    val resultCache = new ResultCache(jobsForCache, fetchers)
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
