package com.kristofszilagyi.pipelineboard.fetchers

import java.time.Instant

import akka.typed.Behavior
import akka.typed.scaladsl.Actor
import com.kristofszilagyi.pipelineboard.FetcherResult
import com.kristofszilagyi.pipelineboard.fetchers.JenkinsFetcher.Fetch
import com.kristofszilagyi.pipelineboard.fetchers.TeamCityFetcher.TCPartialBuildsInfo
import com.kristofszilagyi.pipelineboard.shared._
import com.kristofszilagyi.pipelineboard.utils.TeamCityInstantParser
import com.kristofszilagyi.pipelineboard.utils.Utopia.RichFuture
import io.circe.{Decoder, Encoder}
import io.circe.generic.JsonCodec
import play.api.libs.ws.WSClient
import slogging.LazyLogging

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

final case class TeamCityJob(common: Job)

object TeamCityFetcher {
  private implicit val id: Decoder[Instant] = TeamCityInstantParser.decodeInstant
  private implicit val ie: Encoder[Instant] = io.circe.java8.time.encodeInstant

  @JsonCodec final case class TCPartialBuildInfo(id: BuildNumber, status: TeamCityBuildStatus, state: TeamCityBuildState, startDate: Instant, finishDate: Option[Instant])
  @JsonCodec final case class TCPartialBuildsInfo(build: Seq[TCPartialBuildInfo])
}

//todo cancelled ones are probably missing
final class TeamCityFetcher(ws: WSClient, jobToFetch: TeamCityJob)(implicit ec: ExecutionContext) extends LazyLogging with Fetcher {

  def name: String = {
    val encodedName = Fetcher.encodeForActorName(jobToFetch.common.name.s)
    s"gitLabCi-$encodedName"
  }

  //https://teamcity.jetbrains.com/app/rest/buildTypes/ApacheAnt_BuildAntUsingMave/builds?fields=build(id,number,startDate,finishDate)
  def behaviour: Behavior[JenkinsFetcher.Fetch] = Actor.immutable[Fetch] { case (ctx, msg) =>
    val url = jobToFetch.common.jobInfo
    val acceptHeader = "Accept" -> "application/json"
    ws.url(url.rawString).withHttpHeaders(acceptHeader).get.map { result =>
      safeRead[TCPartialBuildsInfo](jobToFetch.common.jobInfo, result)
    }.lift.onComplete { maybeBuilds =>
      val flattenedResults = maybeBuilds match {
        case Failure(exception) => Left(ResponseError.failedToConnect(url, exception))
        case Success(value) => value
      }
      val resultInStandardFormat = flattenedResults.map(_.build.map{ tcBuildInfo =>
        Right(BuildInfo(tcBuildInfo.state.toBuildStatus(tcBuildInfo.status), tcBuildInfo.startDate, tcBuildInfo.finishDate, tcBuildInfo.id))
      })
      val result = FetcherResult(JobDetails(jobToFetch.common, Some(JobStatus(resultInStandardFormat, Instant.now()))))
      msg.replyTo ! result
    }
    Behavior.same
  }
}
