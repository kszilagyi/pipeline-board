package com.kristofszilagyi.shared

import java.time.{Duration, Instant}
import java.util.concurrent.TimeUnit

import scala.concurrent.duration.FiniteDuration

object InstantOps {
  implicit class RichInstant(instant: Instant) {
    def -(other: Instant): FiniteDuration = {
      val dur = Duration.between(other, instant)
      FiniteDuration(dur.toNanos, TimeUnit.NANOSECONDS)
    }
  }
}
