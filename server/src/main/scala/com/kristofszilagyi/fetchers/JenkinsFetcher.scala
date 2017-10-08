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
import com.kristofszilagyi.utils.Utopia.RichFuture
import io.circe._
import io.circe.generic.JsonCodec
import io.circe.parser.decode
import slogging.LazyLogging

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

@SuppressWarnings(Array(Wart.Public))
object JenkinsJson { //this object is only here because @JsonCodec has the public wart :(
  //todo probably we should have a custom deserializer instead of having an option and do a getOrElse on it
  @JsonCodec final case class PartialDetailedBuildInfo(result: Option[JenkinsBuildStatus], timestamp: Long, duration: Int)

  @JsonCodec final case class PartialBuildInfo(number: Int)
  @JsonCodec final case class PartialJenkinsJobInfo(builds: Seq[PartialBuildInfo])
}

object JenkinsFetcher {

  sealed trait Incoming

  final case class Fetch(job: Seq[Job], replyTo: ActorRef[BulkFetchResult]) extends Incoming

  private final case class JobInfoWithoutBuildInfo(job: Job, jobNumbers: Seq[BuildNumber])

  private final case class JobsInfoWithoutBuildInfo(replyTo: ActorRef[BulkFetchResult], results: Seq[Either[JobDetails, JenkinsFetcher.JobInfoWithoutBuildInfo]]) extends Incoming

  private def restify(u: Uri) = u / "api/json" ? "pretty=true"

  @SuppressWarnings(Array(Wart.AsInstanceOf))
  private def safeRead[T: Decoder](response: WSResponse): Either[ResponseError, T] = {
    if (response.status !=== 200) Left(ResponseError.invalidResponseCode(response))
    else decode[T](response.body).left.map(err => ResponseError.invalidJson(err))
  }
}

//todo add caching
class JenkinsFetcher @Inject()(ws: WSClient)(implicit ec: ExecutionContext) extends LazyLogging {

  @SuppressWarnings(Array(Wart.Null, Wart.Public)) //I think these are false positive
  val behaviour: Actor.Immutable[Incoming] = Actor.immutable[Incoming] { (ctx, msg) =>
    msg match {
      case Fetch(jobs, replyTo) =>
        def fetchJobDetails(job: Job) = {
          val jobUrl = job.uri.u
          val destination = restify(jobUrl)
          ws.url(destination).get.map { safeRead[PartialJenkinsJobInfo] }.lift.noThrowingMap{
            case Success(maybePartialJenkinsJobInfo) => maybePartialJenkinsJobInfo match {
              case Left(error) => Left(JobDetails(job, Left(error)))
              case Right(jenkinsJobInfo) => Right(JobInfoWithoutBuildInfo(
                job,
                jenkinsJobInfo.builds.map(partialBuildInfo => BuildNumber(partialBuildInfo.number))
              ))
            }
            case Failure(t) => Left(JobDetails(job, Left(ResponseError.failedToConnect(t))))
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
        def fetchBuildResults(job: Job, buildNumbers: Seq[BuildNumber]) = {
          val buildInfoFutures = buildNumbers.map { buildNumber =>
            val destination = restify(job.uri.buildInfo(buildNumber))
            ws.url(destination).get.map(result => safeRead[PartialDetailedBuildInfo](result)
              .map { buildInfo =>
                val startTime = Instant.ofEpochMilli(buildInfo.timestamp)
                val endTime = startTime.plusMillis(buildInfo.duration.toLong)
                JenkinsBuildInfo(buildInfo.result.getOrElse(JenkinsBuildStatus.Building), startTime, endTime, buildNumber)
              }
            ).lift noThrowingMap  {
              case Failure(exception) => Left(ResponseError.failedToConnect(exception))
              case Success(value) => value
            }
          }
          Utopia.sequence(buildInfoFutures) noThrowingMap { buildInfo =>
            JobDetails(job, Right(buildInfo))
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


