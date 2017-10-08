package com.kristofszilagyi.shared

import com.netaporter.uri._
import com.netaporter.uri.dsl.uriToUriOps
import io.circe.generic.JsonCodec
import io.circe.{Decoder, Encoder, Error}
import slogging.LazyLogging
import io.circe.disjunctionCodecs._
import UriEncoders._
import cats.syntax.either.{catsSyntaxEither, catsSyntaxEitherObject}
import io.circe._


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


@JsonCodec final case class JobUrl(u: Uri) {
  def buildInfo(buildNumber: BuildNumber): Uri = u /  buildNumber.i.toString
}

@JsonCodec final case class JobName(s: String)

@JsonCodec final case class Job(name: JobName, uri: JobUrl)

@JsonCodec final case class JobDetails(request: Job, r: Either[ResponseError, Seq[scala.Either[ResponseError, JenkinsBuildInfo]]])

@JsonCodec final case class BulkFetchResult(results: Seq[JobDetails])


