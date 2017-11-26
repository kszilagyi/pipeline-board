package com.kristofszilagyi.shared


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
import scala.collection.immutable.ListMap

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
      urls.restRoot.u / "jobs" ? ("per_page" -> 100)
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

  def values: immutable.IndexedSeq[JobType] = findValues
}

/**
  * Url which will be displayed/used with rawString - no encoding or anything
  */
@JsonCodec final case class RawUrl(u: Uri) {
  def /(s: String): RawUrl = RawUrl(u / s)

  def rawString: String = u.toStringRaw

  override def toString: String = rawString
}

@JsonCodec final case class Job(name: JobDisplayName, urls: Urls, tpe: JobType) {
  def buildInfo(n: BuildNumber): RawUrl = tpe.buildInfo(urls, n) //todo this is not implemented on git lab ci, refactor
  def jobInfo: RawUrl = tpe.jobInfo(urls)
  def buildUi(n: BuildNumber): RawUrl = tpe.buildUi(urls, n)
}


@JsonCodec final case class JobStatus(r: Either[ResponseError, Seq[scala.Either[ResponseError, BuildInfo]]], resultTime: Instant)

@JsonCodec final case class JobDetails(static: Job, maybeDynamic: Option[JobStatus])

@JsonCodec final case class JobGroup(jobs: Seq[JobDetails])

@JsonCodec final case class GroupName(s: String)

object CachedResult {
  implicit val keyDecoder = new KeyDecoder[GroupName] {
    def apply(key: String): Option[GroupName] = Some(GroupName(key))
  }
  implicit val keyEncoder = new KeyEncoder[GroupName] {
    def apply(groupName: GroupName): String = groupName.s
  }
}
@JsonCodec final case class CachedResult(groups: ListMap[GroupName, JobGroup])

@JsonCodec final case class ResultAndTime(cachedResult: CachedResult, time: Instant)

