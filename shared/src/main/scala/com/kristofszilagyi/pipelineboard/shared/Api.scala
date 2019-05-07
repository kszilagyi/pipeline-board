package com.kristofszilagyi.pipelineboard.shared


import java.time.Instant

import com.netaporter.uri._
import com.netaporter.uri.dsl.uriToUriOps
import io.circe.generic.JsonCodec
import io.circe.{Decoder, Encoder, Error}
import slogging.LazyLogging
import io.circe.disjunctionCodecs._
import com.netaporter.uri.dsl._
import UriEncoders._
import cats.syntax.either.{catsSyntaxEither, catsSyntaxEitherObject}
import enumeratum.{CirceEnum, Enum, EnumEntry}
import io.circe._
import io.circe.java8.time._

import scala.collection.immutable

@JsonCodec final case class ResponseError(s: String)

object ResponseError extends LazyLogging{
  //todo logs not working
  def invalidJson(error: Error): ResponseError = {
    val msg = "JsonError: " + error.getMessage
    logger.warn(msg)
    ResponseError(msg)
  }

  def failedToConnect(uri: RawUrl, ex: Throwable): ResponseError = {
    val msg = s"Request [${uri.rawString}] failed with exception: " + ex.getMessage
    logger.warn(msg)
    ResponseError(msg)
  }
}

object UriEncoders {
  implicit val uriDecoder: Decoder[Uri] = Decoder.decodeString.emap { str =>
    Either.catchNonFatal(Uri.parse(str)).leftMap(t => s"Deserialising Uri failed with: ${t.getMessage}")
  }

  implicit val uriEncoder: Encoder[Uri] = Encoder.encodeString.contramap[Uri](_.toString)
}

@JsonCodec final case class UserRoot(u: RawUrl)

@JsonCodec final case class RestRoot(u: RawUrl)

@JsonCodec final case class Urls(userRoot: UserRoot, restRoot: RestRoot)

@JsonCodec final case class JobDisplayName(s: String)

sealed trait JobType extends EnumEntry {
  def jobInfo(urls: Urls): RawUrl
  def buildInfo(urls: Urls, n: BuildNumber): RawUrl
  def buildUi(urls: Urls, n: BuildNumber): RawUrl
}

object JobType extends Enum[JobType] with CirceEnum[JobType] {
  case object GitLabCi extends JobType {
    def buildInfo(urls: Urls, n: BuildNumber): RawUrl = ???
    def jobInfo(urls: Urls): RawUrl = {
      (urls.restRoot.u / "jobs") ? ("per_page" -> 100)
    }

    def buildUi(urls: Urls, n: BuildNumber): RawUrl = {
      urls.userRoot.u / "-/jobs" / n.i.toString
    }
  }
  case object Jenkins extends JobType {
    def buildInfo(urls: Urls, n: BuildNumber): RawUrl = {
      urls.userRoot.u / n.i.toString / "api/json"
    }

    def jobInfo(urls: Urls): RawUrl = urls.restRoot.u

    def buildUi(urls: Urls, n: BuildNumber): RawUrl = urls.userRoot.u / n.i.toString
  }

  case object TeamCity extends JobType {
    def jobInfo(urls: Urls): RawUrl = urls.restRoot.u / ("builds?locator=running:any,canceled:any&" +
      "fields=build(id,startDate,finishDate,status,state)")

    def buildInfo(urls: Urls, n: BuildNumber): RawUrl = ??? //todo fix

    def buildUi(urls: Urls, n: BuildNumber): RawUrl = RawUrl(urls.userRoot.u.u.copy(pathParts = Seq(PathPart("viewLog.html"))) & s"buildId=${n.i}")

  }

  def values: immutable.IndexedSeq[JobType] = findValues
}

/**
  * Url which will be displayed/used with rawString - no encoding or anything
  */
@JsonCodec final case class RawUrl(u: Uri) {
  def /(s: String): RawUrl = RawUrl(u / s)
  def &(s: String): RawUrl = RawUrl(u & s)
  def ?(param: (String, Any)): RawUrl = RawUrl(u ? param)

  def rawString: String = u.toStringRaw

  override def toString: String = rawString
}

/*
 * As an old OO guy at heart, I would probably have made JenkinsJob etc *be* a Job not *have* a job - i.e. inheritance
 * going against the old mantra in this case.  (And I would also defend it in principle on the grounds of semantics too.)
 * That would cerainly simplify some of the startup code in Application, but there may be some other consideration which says
 * not to do that.  For example, Job has a @JsonCodec annotation, which XxxSpecificJobs do not, so perhaps that is a bad idea.
 */

@JsonCodec final case class Job(name: JobDisplayName, urls: Urls, tpe: JobType) {
  def buildInfo(n: BuildNumber): RawUrl = tpe.buildInfo(urls, n) //todo this is not implemented on git lab ci, refactor
  def jobInfo: RawUrl = tpe.jobInfo(urls)
  def buildUi(n: BuildNumber): RawUrl = {
    tpe.buildUi(urls, n)
  }
}


@JsonCodec final case class JobBuilds(r: Either[ResponseError, Seq[scala.Either[ResponseError, BuildInfo]]], resultTime: Instant)

@JsonCodec final case class JobDetails(jobDescription: Job, builds: JobBuilds)

@JsonCodec final case class JobGroup(jobs: Seq[JobDetails])

@JsonCodec final case class GroupName(s: String)

object AllGroups {
  implicit val keyDecoder = new KeyDecoder[GroupName] {
    def apply(key: String): Option[GroupName] = Some(GroupName(key))
  }
  implicit val keyEncoder = new KeyEncoder[GroupName] {
    def apply(groupName: GroupName): String = groupName.s
  }
}
@JsonCodec final case class AllGroups(groups: Seq[(GroupName, JobGroup)])

@JsonCodec final case class ResultAndTime(allGroups: AllGroups, time: Instant)

