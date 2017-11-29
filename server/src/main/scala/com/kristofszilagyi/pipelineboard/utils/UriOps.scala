package com.kristofszilagyi.pipelineboard.utils

import com.netaporter.uri.Uri
import com.netaporter.uri.config.UriConfig
import com.netaporter.uri.decoding.NoopDecoder
import com.netaporter.uri.encoding.NoopEncoder

import scala.util.Try

object UriOps {
  final case class NotAUri(s: String)
  implicit class RichUriObj(u: Uri.type) {
    def safeParse(s: String): Either[NotAUri, Uri] = {
      implicit val parseConfig: UriConfig = UriConfig(NoopEncoder, NoopDecoder)

      Try(Uri.parse(s)).toEither.left.map(t => NotAUri(s"Parsing [$s] has failed with ${t.getMessage}"))
    }
  }
}
