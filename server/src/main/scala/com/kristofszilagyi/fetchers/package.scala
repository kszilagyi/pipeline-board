package com.kristofszilagyi

import akka.typed.ActorRef
import com.kristofszilagyi.shared.{FetcherResult, ResponseError, Wart}
import io.circe.Decoder
import play.api.libs.ws.WSResponse
import com.kristofszilagyi.shared.TypeSafeEqualsOps._
import com.kristofszilagyi.utils.ResponseErrorUtils._
import com.netaporter.uri.Uri
import io.circe.parser.decode

package object fetchers {
  @SuppressWarnings(Array(Wart.AsInstanceOf))
  def safeRead[T: Decoder](uri: Uri, response: WSResponse): Either[ResponseError, T] = {
    if (response.status !=== 200) Left(ResponseError.invalidResponseCode(uri, response))
    else decode[T](response.body).left.map(err => ResponseError.invalidJson(err))
  }

}

