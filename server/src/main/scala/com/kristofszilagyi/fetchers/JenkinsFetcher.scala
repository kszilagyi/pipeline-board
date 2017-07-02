package com.kristofszilagyi.fetchers

import javax.inject.Inject

import akka.typed._
import akka.typed.scaladsl.Actor
import com.kristofszilagyi.Wart
import com.kristofszilagyi.fetchers.JenkinsFetcher._
import com.kristofszilagyi.utils.FutureUtils.RichFuture
import com.netaporter.uri.Uri
import com.netaporter.uri.dsl._
import play.api.libs.ws._
import upickle.default._
import com.kristofszilagyi.utils.TypeSafeEqualsOps._
import upickle.Invalid

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

sealed abstract class BuildStatus(val repr: String)

case object Building extends BuildStatus("building")
case object Failed extends BuildStatus("failed")
case object Successful extends BuildStatus("success")
case object Aborted extends BuildStatus("aborted")

final case class PartialDetailedBuildInfo(result: BuildStatus)


final case class JenkinsJobUrl(url: Uri) {
  def buildInfo(buildNumber: BuildNumber): Uri = url / buildNumber.i.toString
}

final case class BuildNumber(i: Int)


final case class DeserialisationError(desc: String)

final case class PartialBuildInfo(number: Int)
final case class PartialJenkinsJobInfo(builds: Seq[PartialBuildInfo])

object JenkinsFetcher {
  final case class FetchResult(r: Try[Either[ResponseError, Seq[Try[scala.Either[ResponseError, BuildStatus]]]]])
  sealed trait Incoming
  final case class Fetch(job: JenkinsJobUrl, replyTo: ActorRef[FetchResult]) extends Incoming
  private final case class FirstSuccessful(job: JenkinsJobUrl, jobNumbers: Seq[BuildNumber],
                                        replyTo: ActorRef[FetchResult]) extends Incoming

  private def restify(u: Uri) = u / "api/json?pretty=true"

  @SuppressWarnings(Array(Wart.AsInstanceOf))
  private def safeRead[T: Reader](body: WSResponse): Either[ResponseError, T] = {
    if (body.status !=== 200) Left(InvalidResponseCode(body))
    else {
      Try(read[T](body.body)) match {
        case Failure(exception) => Left(UPickleError(exception.asInstanceOf[upickle.Invalid])) //the docs said so :(
        case Success(value) => Right(value)
      }
    }
  }
}

sealed trait ResponseError
final case class UPickleError(invalid: Invalid) extends ResponseError
final case class InvalidResponseCode(body: WSResponse) extends ResponseError

class JenkinsFetcher @Inject() (ws: WSClient)(implicit ec: ExecutionContext) {

  @SuppressWarnings(Array(Wart.Null, Wart.Public)) //I think these are false positive
  val behaviour: Actor.Immutable[Incoming] = Actor.immutable[Incoming] { (ctx, msg) =>
    msg match {
      case Fetch(job, replyTo) =>
        val future = ws.url(restify(job.url)).get.map(response => safeRead[PartialJenkinsJobInfo](response))

        future onComplete {
          case Failure(ex) => replyTo ! FetchResult(Failure(ex))
          case Success(Left(error)) => replyTo ! FetchResult(Success(Left(error)))
          case Success(Right(jenkinsJobInfo)) => ctx.self ! FirstSuccessful(
            job,
            jenkinsJobInfo.builds.map(partialBuildInfo => BuildNumber(partialBuildInfo.number)),
            replyTo
          )
        }
        Actor.same
      case FirstSuccessful(job, buildNumbers, replyTo) =>
        val liftedFutures = buildNumbers.map{ buildNumber =>
            ws.url(restify(job.buildInfo(buildNumber))).get.map(result => safeRead[PartialDetailedBuildInfo](result).map(_.result)).lift
          }
        Future.sequence(liftedFutures) foreach { buildStatuses => //this future can't fail because all the futures are lifted#
          replyTo ! FetchResult(Success(Right(buildStatuses)))
        }
        Actor.same
    }
  }
}


