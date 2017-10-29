package com.kristofszilagyi.utils

import com.kristofszilagyi.shared.ResponseError
import com.netaporter.uri.Uri
import play.api.libs.ws.WSResponse

object ResponseErrorUtils {
  implicit class RichResponseError(obj: ResponseError.type) {
    def invalidResponseCode(uri: Uri, wsResponse: WSResponse): ResponseError = {
      //todo security risk, good for debugging though
      obj(s"Invalid response code for request [$uri]: " + wsResponse.status.toString + ", body: " + wsResponse.body)
    }
  }
}
