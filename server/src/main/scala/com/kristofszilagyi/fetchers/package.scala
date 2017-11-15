package com.kristofszilagyi

import com.kristofszilagyi.shared.{JobDetails, RawUrl, ResponseError, Wart}
import io.circe.Decoder
import play.api.libs.ws.WSResponse
import com.kristofszilagyi.shared.TypeSafeEqualsOps._
import com.kristofszilagyi.utils.ResponseErrorUtils._
import io.circe.parser.decode

package object fetchers {
  @SuppressWarnings(Array(Wart.AsInstanceOf))
  def safeRead[T: Decoder](uri: RawUrl, response: WSResponse): Either[ResponseError, T] = {
    if (response.status !=== 200) Left(ResponseError.invalidResponseCode(uri, response))
    else decode[T](response.body).left.map(err => ResponseError.invalidJson(err))
  }

}

final case class FetcherResult(result: JobDetails)
