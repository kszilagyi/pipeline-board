package com.kristofszilagyi.utils

import com.kristofszilagyi.shared.ResponseError
import play.api.libs.ws.WSResponse

object ResponseErrorUtils {
  implicit class RichResponseError(obj: ResponseError.type) {
    def invalidResponseCode(wsResponse: WSResponse): ResponseError = {
      obj("Invalid response code: " + wsResponse.status.toString + ", body: " + wsResponse.body)
    }
  }
}
