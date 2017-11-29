package com.kristofszilagyi.pipelineboard.shared

import java.time.{Duration, Instant}
import java.util.concurrent.TimeUnit

import scala.concurrent.duration.FiniteDuration

object InstantOps {
  @SuppressWarnings(Array(Wart.Overloading))
  implicit class RichInstant(instant: Instant) {
    def -(other: Instant): FiniteDuration = {
      val dur = Duration.between(other, instant)
      FiniteDuration(dur.toNanos, TimeUnit.NANOSECONDS)
    }

    def -(duration: FiniteDuration): Instant = {
      instant.minusNanos(duration.toNanos)
    }

    def +(duration: FiniteDuration): Instant = {
      instant.plusNanos(duration.toNanos)
    }
  }
}
