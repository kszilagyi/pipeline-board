package com.kristofszilagyi.utils

import com.kristofszilagyi.shared.{RawUrl, ResponseError}
import play.api.libs.ws.WSResponse

object ResponseErrorUtils {
  implicit class RichResponseError(obj: ResponseError.type) {
    def invalidResponseCode(uri: RawUrl, wsResponse: WSResponse): ResponseError = {
      //todo security risk, good for debugging though
      obj(s"Invalid response code for request [${uri.rawString}]: " + wsResponse.status.toString + ", body: " + wsResponse.body)
    }
  }
}
