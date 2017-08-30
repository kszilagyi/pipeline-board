package com.kristofszilagyi.shared

import java.time.ZonedDateTime

import scala.concurrent.duration.{Duration, FiniteDuration}

object ZonedDateTimeOps {
  implicit class RichZonedDateTime(zonedDateTime: ZonedDateTime) {
    def -(finiteDuration: FiniteDuration): ZonedDateTime = {
      ZonedDateTime.now().minusNanos(finiteDuration.toNanos)
    }

    def +(duration: Duration): ZonedDateTime = {
      ZonedDateTime.now().plusNanos(duration.toNanos)
    }
  }
}
