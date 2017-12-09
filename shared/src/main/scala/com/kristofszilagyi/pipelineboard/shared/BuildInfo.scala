package com.kristofszilagyi.pipelineboard.shared

import java.time.Instant

import io.circe.{Decoder, Encoder}
import io.circe.generic.JsonCodec
import io.circe.java8.time._

object JsonUtils {
  def wrappedIntDecoder[T](create: Int => T): Decoder[T] = Decoder.decodeInt.map { i =>
    create(i)
  }

  def wrappedIntEncoder[T](unwrap: T => Int): Encoder[T] = Encoder.encodeInt.contramap[T](unwrap)
}

object BuildNumber {
  implicit val encoder: Encoder[BuildNumber] = JsonUtils.wrappedIntEncoder[BuildNumber](_.i)
  implicit val decoder: Decoder[BuildNumber] = JsonUtils.wrappedIntDecoder[BuildNumber](BuildNumber.apply)
}

final case class BuildNumber(i: Int)

@JsonCodec final case class BuildInfo(buildStatus: BuildStatus,
                                      buildStart: Instant,
                                      maybeBuildFinish: Option[Instant],
                                      buildNumber: BuildNumber)
