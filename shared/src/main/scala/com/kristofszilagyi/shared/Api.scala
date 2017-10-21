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

@JsonCodec final case class ResponseError(s: String)

object ResponseError extends LazyLogging{
  //todo logs not working
  def invalidJson(error: Error): ResponseError = {
    val msg = "JsonError: " + error.getMessage
    logger.warn(msg)
    ResponseError(msg)
  }

  def failedToConnect(ex: Throwable): ResponseError = {
    val msg = "Request failed with exception: " + ex.getMessage
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

@JsonCodec final case class UserRoot(u: Uri)

@JsonCodec final case class RestRoot(u: Uri)

@JsonCodec final case class Urls(userRoot: UserRoot, restRoot: RestRoot)

@JsonCodec final case class JobName(s: String)

sealed trait JobType extends EnumEntry {
  def buildInfo(urls: Urls, n: BuildNumber): Uri
}

object JobType extends Enum[JobType] with CirceEnum[JobType] {
  case object GitLabCi extends JobType {
    def buildInfo(urls: Urls, n: BuildNumber): Uri = ???
  }
  case object Jenkins extends JobType {
    def buildInfo(urls: Urls, n: BuildNumber): Uri = {
      urls.userRoot.u / n.i.toString / "api/json"
    }
  }

  def values: immutable.IndexedSeq[JobType] = findValues
}


@JsonCodec final case class Job(name: JobName, urls: Urls, tpe: JobType) {
  def buildInfo(n: BuildNumber): Uri = tpe.buildInfo(urls, n)
}

@JsonCodec final case class JobDetails(request: Job, r: Either[ResponseError, Seq[scala.Either[ResponseError, BuildInfo]]])

@JsonCodec final case class FetcherResult(results: Seq[JobDetails])

@JsonCodec final case class AllResult(results: Seq[JobDetails], resultTime: Instant)
@JsonCodec final case class CachedResult(maybe: Option[AllResult])

@JsonCodec final case class ResultAndTime(cachedResult: CachedResult, time: Instant)

