package com.kristofszilagyi.utils

import enumeratum.{Circe, Enum, EnumEntry}
import io.circe.{Decoder, Encoder}

trait TolerantCirceEnum[A <: EnumEntry] { this: Enum[A] =>
  implicit val circeEncoder: Encoder[A] = Circe.encoder(this)
  implicit val circeDecoder: Decoder[A] = Circe.decodeCaseInsensitive(this)
}