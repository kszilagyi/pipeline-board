package com.kristofszilagyi.utils

import com.netaporter.uri.Uri
import io.circe.Encoder
import io.circe._
import cats.syntax.either._

object UriUtils {
  implicit val encoder: Encoder[Uri] = Encoder.encodeString.contramap[Uri](_.toString)
  implicit val decoder: Decoder[Uri] = Decoder.decodeString.emap { str =>
    Either.catchNonFatal(Uri.parse(str)).leftMap(_.getMessage)
  }

}
