package com.kristofszilagyi.fetchers

import javax.inject.Inject

import com.kristofszilagyi.fetchers.DEither.{DEither, RichJsResult}
import com.kristofszilagyi.fetchers.JenkinsFetcher.restify
import com.kristofszilagyi.utils.EitherUtils.RichEitherOfFuture
import com.kristofszilagyi.utils.FutureUtils.RichFuture
import com.netaporter.uri.Uri
import com.netaporter.uri.dsl._
import play.api.libs.json._
import play.api.libs.ws._
import com.kristofszilagyi.utils.TypeSafeEqualsOps._

import scala.concurrent.{ExecutionContext, Future}

object BuildStatus {
  def all: Traversable[BuildStatus] = Seq(Building, Failed, Success, Aborted)
  implicit val reader: Reads[BuildStatus] = Reads[BuildStatus] { json =>
    JsPath.read[String].reads(json).flatMap { s =>
      all.filter(_.repr ==== s).toSeq match {
        case Seq(b) => JsSuccess(b)
        case _ => JsError(JsonValidationError(s"BuildStatus should be one of $all but was $s"))
      }
    }
  }
}

sealed abstract class BuildStatus(val repr: String)

case object Building extends BuildStatus("building")
case object Failed extends BuildStatus("failed")
case object Success extends BuildStatus("success")
case object Aborted extends BuildStatus("aborted")

final case class BuildData(result: DEither[BuildStatus])

final case class JobData(allBuilds: Seq[DEither[BuildData]])

final case class JenkinsJobUrl(url: Uri)

object JobNumber {
  implicit val reader: Reads[JobNumber] = JsPath.read[Int].map(JobNumber.apply)
}
final case class JobNumber(i: Int)

object DEither {
  type DEither[T] = Either[DeserialisationError, T]
  implicit class RichJsResult[T](jsResult: JsResult[T]) {
    def toDEither: DEither[T] = jsResult match {
      case JsSuccess(value, path) => Right(value)
      case e : JsError => Left(DeserialisationError(e))
    }
  }
}

object DeserialisationError {
  def apply(e: JsError): DeserialisationError = {
    DeserialisationError(JsError.toJson(e).toString())
  }
}

final case class DeserialisationError(desc: String)



object JenkinsFetcher {
  private def restify(u: Uri) = u / "api/json?pretty=true"
}

class JenkinsFetcher @Inject() (ws: WSClient)(implicit ec: ExecutionContext) {
  def query(job: JenkinsJobUrl): Unit = {
    val buildNumbers = ws.url(restify(job.url)).get.map(response => (response.json \ "builds" \\ "number")
      .map(_.validate[JobNumber].toDEither)).lift
    val x = buildNumbers.map(_.map{numbers =>
      val buildResults = numbers.map(_.map{
          num => ws.url(restify(job.url / num.toString)).get.map(_.json \ "result").map(_.validate[BuildStatus].toDEither)
      })
      Future.sequence(buildResults.map(_.flipLift))
    })
    buildNumbers.foreach(println)
   // ???
  }
}


