package com.kristofszilagyi.utils

import com.netaporter.uri.Uri

import scala.util.Try

object UriOps {
  final case class NotAUri(s: String)
  implicit class RichUriObj(u: Uri.type) {
    def safeParse(s: String): Either[NotAUri, Uri] = {
      Try(Uri.parse(s)).toEither.left.map(t => NotAUri(s"Parsing [$s] has failed with ${t.getMessage}"))
    }
  }
}
