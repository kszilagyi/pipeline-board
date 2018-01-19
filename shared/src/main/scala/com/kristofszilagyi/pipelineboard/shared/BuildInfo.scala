package com.kristofszilagyi.pipelineboard.shared

import java.time.Instant

import com.kristofszilagyi.pipelineboard.shared.BuildStatus.Successful
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

object BuildInfo {
  def successful(start: Instant,
                 finish: Instant,
                 buildNumber: BuildNumber): BuildInfo = {
    BuildInfo(Successful, start, Some(finish), buildNumber)
  }
}

@JsonCodec final case class BuildInfo(buildStatus: BuildStatus,
                                      buildStart: Instant,
                                      maybeBuildFinish: Option[Instant],
                                      buildNumber: BuildNumber) {
  def overlap(other: BuildInfo): Boolean = {
    (buildStart.isBefore(other.buildStart) && maybeBuildFinish.getOrElse(Instant.MAX).isAfter(other.buildStart)) ||
      (buildStart.isAfter(other.buildStart) && buildStart.isBefore(other.maybeBuildFinish.getOrElse(Instant.MAX)))
  }
}
