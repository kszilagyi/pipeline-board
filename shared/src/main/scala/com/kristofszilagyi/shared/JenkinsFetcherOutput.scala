package com.kristofszilagyi.shared

import io.circe.generic.JsonCodec
import io.circe.disjunctionCodecs._
import io.circe.Error

import slogging.LazyLogging

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

@JsonCodec final case class Url(s: String)


@JsonCodec final case class FetchResult(request: Url, r: Either[ResponseError, Seq[scala.Either[ResponseError, JenkinsBuildInfo]]])

@JsonCodec final case class BulkFetchResult(results: Seq[FetchResult])


