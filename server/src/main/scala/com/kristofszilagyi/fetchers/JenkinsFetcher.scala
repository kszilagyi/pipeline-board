package com.kristofszilagyi.fetchers

import java.time.Instant
import javax.inject.Inject

import akka.typed._
import akka.typed.scaladsl.Actor
import com.kristofszilagyi.fetchers.JenkinsFetcher._
import com.kristofszilagyi.fetchers.JenkinsJson.{PartialDetailedBuildInfo, PartialJenkinsJobInfo}
import com.kristofszilagyi.shared._
import com.kristofszilagyi.utils.ResponseErrorUtils.RichResponseError
import com.netaporter.uri.Uri
import com.netaporter.uri.dsl._
import play.api.libs.ws._
import TypeSafeEqualsOps._
import com.kristofszilagyi.utils.Utopia
import com.kristofszilagyi.utils.UrlOps.RichUri
import com.kristofszilagyi.utils.Utopia.RichFuture
import io.circe._
import io.circe.generic.JsonCodec
import io.circe.parser.decode
import slogging.LazyLogging

import scala.concurrent.ExecutionContext

@SuppressWarnings(Array(Wart.Public))
object JenkinsJson { //this object is only here because @JsonCodec has the public wart :(
  //todo probably we should have a custom deserializer instead of having an option and do a getOrElse on it
  @JsonCodec final case class PartialDetailedBuildInfo(result: Option[JenkinsBuildStatus], timestamp: Long, duration: Int)

  @JsonCodec final case class PartialBuildInfo(number: Int)
  @JsonCodec final case class PartialJenkinsJobInfo(builds: Seq[PartialBuildInfo])
}

final case class JenkinsJobUrl(uri: Uri) {
  def buildInfo(buildNumber: BuildNumber): Uri = uri / buildNumber.i.toString
}


object JenkinsFetcher {

  sealed trait Incoming

  final case class Fetch(job: Seq[JenkinsJobUrl], replyTo: ActorRef[BulkFetchResult]) extends Incoming

  private final case class JobInfoWithoutBuildInfo(job: JenkinsJobUrl, jobNumbers: Seq[BuildNumber])

  private final case class JobsInfoWithoutBuildInfo(replyTo: ActorRef[BulkFetchResult], results: Seq[Either[JobDetails, JenkinsFetcher.JobInfoWithoutBuildInfo]]) extends Incoming

  private def restify(u: Uri) = u / "api/json" ? "pretty=true"

  @SuppressWarnings(Array(Wart.AsInstanceOf))
  private def safeRead[T: Decoder](response: WSResponse): Either[ResponseError, T] = {
    if (response.status !=== 200) Left(ResponseError.invalidResponseCode(response))
    else decode[T](response.body).left.map(err => ResponseError.invalidJson(err))
  }
}

class JenkinsFetcher @Inject()(ws: WSClient)(implicit ec: ExecutionContext) extends LazyLogging {

  @SuppressWarnings(Array(Wart.Null, Wart.Public)) //I think these are false positive
  val behaviour: Actor.Immutable[Incoming] = Actor.immutable[Incoming] { (ctx, msg) =>
    msg match {
      case Fetch(jobs, replyTo) =>
        def fetchJobDetails(job: JenkinsJobUrl) = {
          val jobUrl = job.uri.toUrl
          val destination = restify(job.uri)
          ws.url(destination).get.map { safeRead[PartialJenkinsJobInfo] }.lift.noThrowingMap{
            case Right(maybePartialJenkinsJobInfo) => maybePartialJenkinsJobInfo match {
              case Left(error) => Left(JobDetails(jobUrl, Left(error)))
              case Right(jenkinsJobInfo) => Right(JobInfoWithoutBuildInfo(
                job,
                jenkinsJobInfo.builds.map(partialBuildInfo => BuildNumber(partialBuildInfo.number))
              ))
            }
            case Left(t) => Left(JobDetails(jobUrl, Left(ResponseError.failedToConnectS(t))))
          }
        }

        val future = Utopia.sequence(jobs.map { job =>
          fetchJobDetails(job)
        })
        future onComplete {
          ctx.self ! JobsInfoWithoutBuildInfo(replyTo, _)
        }

        Actor.same
      case JobsInfoWithoutBuildInfo(replyTo, jobs) =>
        def fetchBuildResults(jenkinsJobUrl: JenkinsJobUrl, buildNumbers: Seq[BuildNumber]) = {
          val jobUrl = jenkinsJobUrl.uri.toUrl
          val buildInfoFutures = buildNumbers.map { buildNumber =>
            val destination = restify(jenkinsJobUrl.buildInfo(buildNumber))
            ws.url(destination).get.map(result => safeRead[PartialDetailedBuildInfo](result)
              .map { buildInfo =>
                val startTime = Instant.ofEpochMilli(buildInfo.timestamp)
                val endTime = startTime.plusMillis(buildInfo.duration.toLong)
                JenkinsBuildInfo(buildInfo.result.getOrElse(JenkinsBuildStatus.Building), startTime, endTime, buildNumber)
              }
            ).lift noThrowingMap  {
              case Left(exception) => Left(ResponseError.failedToConnectS(exception))
              case Right(value) => value
            }
          }
          Utopia.sequence(buildInfoFutures) noThrowingMap { buildInfo =>
            JobDetails(jobUrl, Right(buildInfo))
          }
        }
        val futureResults = jobs.map {
          case Left(fetchResult) => Utopia.finished(fetchResult)
          case Right(JobInfoWithoutBuildInfo(job, buildNumbers)) =>
            fetchBuildResults(job, buildNumbers)
        }
        Utopia.sequence(futureResults) onComplete {
          replyTo ! BulkFetchResult(_)
        }
        Actor.same
    }
  }
}


