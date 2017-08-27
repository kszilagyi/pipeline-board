package com.kristofszilagyi.shared

import io.circe.generic.JsonCodec
import io.circe.disjunctionCodecs._
import io.circe.Error

@JsonCodec final case class ResponseError(s: String)

object ResponseError {
  def invalidJson(error: Error): ResponseError = {
    ResponseError("JsonError: " + error.getMessage)
  }

  def failedToConnect(ex: Throwable): ResponseError = {
    ResponseError("Request failed with exception: " + ex.getMessage)
  }
}

@JsonCodec final case class Url(s: String)


@JsonCodec final case class FetchResult(request: Url, r: Either[ResponseError, Seq[scala.Either[ResponseError, JenkinsBuildInfo]]])


