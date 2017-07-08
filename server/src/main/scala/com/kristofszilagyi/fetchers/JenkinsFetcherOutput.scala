package com.kristofszilagyi.fetchers

import com.kristofszilagyi.utils.UriUtils
import com.netaporter.uri.Uri
import enumeratum.{CirceEnum, Enum, EnumEntry}
import io.circe.Encoder
import play.api.libs.ws.WSResponse
import io.circe._
import io.circe.generic.{JsonCodec, semiauto}
import disjunctionCodecs._
import io.circe.generic.semiauto._

import scala.collection.immutable

sealed abstract class BuildStatus(override val entryName: String) extends EnumEntry

object BuildStatus extends Enum[BuildStatus] with CirceEnum[BuildStatus] {
  val values: immutable.IndexedSeq[BuildStatus] = findValues

  case object Building extends BuildStatus("BUILDING")
  case object Failed extends BuildStatus("FAILED")
  case object Successful extends BuildStatus("SUCCESS")
  case object Aborted extends BuildStatus("ABORTED")
}

@JsonCodec final case class ResponseError(s: String)

object ResponseError {
  def invalidJson(error: Error): ResponseError = {
    ResponseError("JsonError: " + error.getMessage)
  }

  def invalidResponseCode(wSResponse: WSResponse): ResponseError = {
    ResponseError("Invalid response code: " + wSResponse.status.toString + ", body: " + wSResponse.body)
  }

  def failedToConnect(ex: Throwable): ResponseError = {
    ResponseError("Request failed with exception: " + ex.getMessage)
  }
}

final case class ErrorAndRequest(request: Uri, responseError: ResponseError)

object ErrorAndRequest {
  implicit val enc: Encoder[Uri] = UriUtils.encoder // tell me, why does import not work?!
  implicit val dec: Decoder[Uri] = UriUtils.decoder // tell me, why does import not work?!
  implicit val encoder: Encoder[ErrorAndRequest] = deriveEncoder
  implicit val decoder: Decoder[ErrorAndRequest] = deriveDecoder
}

@JsonCodec final case class FetchResult(r: Either[ErrorAndRequest, Seq[scala.Either[ErrorAndRequest, BuildStatus]]])


