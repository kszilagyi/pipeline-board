package com.kristofszilagyi.fetchers

import java.sql.Timestamp
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject

import akka.typed._
import akka.typed.scaladsl.Actor
import com.kristofszilagyi.{shared, _}
import com.kristofszilagyi.fetchers.JenkinsFetcher._
import com.kristofszilagyi.fetchers.JenkinsJson.{PartialDetailedBuildInfo, PartialJenkinsJobInfo}
import com.kristofszilagyi.shared._
import com.kristofszilagyi.utils.FutureUtils.RichFuture
import com.kristofszilagyi.utils.ResponseErrorUtils.RichResponseError
import com.netaporter.uri.Uri
import com.netaporter.uri.dsl._
import play.api.libs.ws._
import TypeSafeEqualsOps._
import com.kristofszilagyi.utils.LiftedFuture
import com.kristofszilagyi.utils.UrlOps.RichUri
import io.circe._
import io.circe.generic.JsonCodec
import io.circe.parser.decode
import slogging.LazyLogging

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@SuppressWarnings(Array(Wart.Public))
object JenkinsJson { //this object is only here because @JsonCodec has the public wart :(
  @JsonCodec final case class PartialDetailedBuildInfo(result: JenkinsBuildStatus, timestamp: Long, duration: Int)

  @JsonCodec final case class PartialBuildInfo(number: Int)
  @JsonCodec final case class PartialJenkinsJobInfo(builds: Seq[PartialBuildInfo])
}

final case class JenkinsJobUrl(uri: Uri) {
  def buildInfo(buildNumber: BuildNumber): Uri = uri / buildNumber.i.toString
}


object JenkinsFetcher {

  sealed trait Incoming

  final case class Fetch(job: Seq[JenkinsJobUrl], replyTo: ActorRef[BulkFetchResult]) extends Incoming

  private final case class FirstSuccessful(job: JenkinsJobUrl, jobNumbers: Seq[BuildNumber])

  private final case class FirstSuccessfulBulk(replyTo: ActorRef[BulkFetchResult], results: Seq[Either[FetchResult, JenkinsFetcher.FirstSuccessful]]) extends Incoming

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
        val future = LiftedFuture.sequence(jobs.map { job =>
          val jobUrl = job.uri.toUrl
          val destination = restify(job.uri)
          ws.url(destination).get.lift.map {
            case Success(response) => safeRead[PartialJenkinsJobInfo](response) match {
              case Left(error) => Left(FetchResult(jobUrl, Left(error)))
              case Right(jenkinsJobInfo) => Right(FirstSuccessful(
                job,
                jenkinsJobInfo.builds.map(partialBuildInfo => BuildNumber(partialBuildInfo.number))
              ))
            }
            case Failure(t) => Left(FetchResult(jobUrl, Left(ResponseError.failedToConnect(t))))
          }
        }).map(res => FirstSuccessfulBulk(replyTo, res))

        future onComplete { ctx.self ! _ }

        Actor.same
      case FirstSuccessfulBulk(replyTo, results) =>
        val futureResults = results.map {
          case Left(fetchResult) => LiftedFuture.successful(fetchResult)
          case Right(FirstSuccessful(job, buildNumbers)) =>
            val jobUrl = job.uri.toUrl
            val liftedFutures = buildNumbers.map { buildNumber =>
              val destination = restify(job.buildInfo(buildNumber))
              ws.url(destination).get.map(result => safeRead[PartialDetailedBuildInfo](result)
                .map { buildInfo =>
                  val startTime = Instant.ofEpochMilli(buildInfo.timestamp)
                  val endTime = startTime.plusMillis(buildInfo.duration.toLong)
                  JenkinsBuildInfo(buildInfo.result, startTime, endTime, buildNumber)
                }).lift map {
                case Failure(exception) => Left(ResponseError.failedToConnect(exception))
                case Success(value) => value
              }
            }
            LiftedFuture.sequence(liftedFutures) map { buildInfo => //this future can't fail because all the futures are lifted#
              FetchResult(jobUrl, Right(buildInfo))
            }
        }
        LiftedFuture.sequence(futureResults) onComplete {
          replyTo ! BulkFetchResult(_)
        }
        Actor.same
    }
  }
}


