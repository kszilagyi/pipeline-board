package com.kristofszilagyi.pipelineboard.controllers

import java.io.File
import java.net.URLEncoder
import javax.inject._

import com.kristofszilagyi.pipelineboard.actors.ResultCache
import com.kristofszilagyi.pipelineboard.fetchers._
import com.kristofszilagyi.pipelineboard.shared.CssSettings.settings._
import com.kristofszilagyi.pipelineboard.shared.JobType.{GitLabCi, Jenkins, TeamCity}
import com.kristofszilagyi.pipelineboard.shared._
import com.netaporter.uri.{EmptyQueryString, PathPart}
import net.jcazevedo.moultingyaml.PimpedString
import play.api.Configuration
import play.api.libs.ws.WSClient
import play.api.mvc._
import com.netaporter.uri.dsl._
import slogging.{LazyLogging, LoggerConfig, PrintLoggerFactory}
import TypeSafeEqualsOps._
import Application._
import scala.collection.immutable.{ListMap, ListSet}
import scala.concurrent.ExecutionContext
import scala.io.Source

object Application {
  private val utf8 = "utf-8"
}

class Application @Inject() (wsClient: WSClient)(val config: Configuration)
                                             (implicit ec: ExecutionContext) extends InjectedController with LazyLogging{

  LoggerConfig.factory = PrintLoggerFactory()

  @SuppressWarnings(Array(Wart.Throw))
  private val jobConfig = {
    //todo fix for other OS
    val home = System.getenv("HOME")
    val primaryConfigPath = new File("config")
    val secondaryConfigPath = new File(s"$home/.pipeline_board/config")
    val configPath = if (primaryConfigPath.isFile) primaryConfigPath
    else if (secondaryConfigPath.exists()) secondaryConfigPath
    else throw new RuntimeException(s"Neither $primaryConfigPath (in working dir) nor $secondaryConfigPath exists, aborting.")
    logger.info(s"Using config: $configPath")
    val config = Config.format.read(Source.fromFile(configPath).mkString.parseYaml)
    logger.info(s"Config is: $config")
    config
  }

  def root: Action[AnyContent] = Action {
    Ok(views.html.index(jobConfig.title)(config))
  }

  def css: Action[AnyContent] = Action {
    Ok(MyStyles.render).as(CSS)
  }

  @SuppressWarnings(Array(Wart.Throw))
  private val autowireServer = {
    val groupedJobs = ListMap(jobConfig.groups.map { group =>
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
        val userRoot = jobConfig.url
        val jobPath = URLEncoder.encode(userRoot.u.u.pathParts.map(_.part).mkString("/"), utf8)
        val restRoot = userRoot.u.u.copy(pathParts = Seq(PathPart("api"), PathPart("v4"), PathPart("projects")) :+ PathPart(jobPath)) //todo url encode
        GitLabCiJob(
          Job(jobConfig.name, Urls(userRoot = userRoot, restRoot = RestRoot(RawUrl(restRoot))), GitLabCi),
          jobConfig.accessToken, jobConfig.jobNameOnGitLab
        )
      }

      val teamCityJobs = group.teamCity.map(_.jobs).getOrElse(Seq.empty).map { jobConfig =>
        val userRoot = jobConfig.url
        val jobId = userRoot.u.u.query.param("buildTypeId").getOrElse(throw new RuntimeException("Url for TeamCity has to have a buildTypeId parameter"))
        val restRoot = userRoot.u.u.copy(pathParts = Seq("guestAuth", "app", "rest", "buildTypes", jobId).map(PathPart.apply),
          query = EmptyQueryString)
        TeamCityJob(
          Job(jobConfig.name, Urls(userRoot = userRoot, restRoot = RestRoot(RawUrl(restRoot))), TeamCity)
        )
      }
      (group.groupName, (jenkinsJobs, gitLabJobs, teamCityJobs))
    }: _*)

    val jenkinsJobs = groupedJobs.toList.flatMap(_._2._1)
    val gitLabJobs = groupedJobs.toList.flatMap(_._2._2)
    val teamCityJobs = groupedJobs.toList.flatMap(_._2._3)
    val fetchers =
      jenkinsJobs.map(new JenkinsFetcher(wsClient, _)) ++ gitLabJobs.map(new GitLabCiFetcher(wsClient, _)) ++
        teamCityJobs.map(new TeamCityFetcher(wsClient, _))

    val jobs = jenkinsJobs.map(_.common) ++ gitLabJobs.map(_.common)
    val jobSet = ListSet(jobs: _*)
    assert(jobs.size ==== jobSet.size, "Jobs are not unique")
    assert(jobs.map(_.name).toSet.size ==== jobs.map(_.name).size, "Jobs names are not unique")
    val jobsForCache = groupedJobs.map{case (name, (jenkins, gitlab, teamCity)) =>
        name -> (jenkins.map(_.common) ++ gitlab.map(_.common) ++ teamCity.map(_.common))
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
