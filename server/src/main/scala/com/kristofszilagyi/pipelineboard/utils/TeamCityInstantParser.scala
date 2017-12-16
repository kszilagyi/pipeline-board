package com.kristofszilagyi.pipelineboard.utils

import java.time.Instant
import java.time.format.{DateTimeFormatter, DateTimeParseException}

import com.kristofszilagyi.pipelineboard.shared.Wart
import io.circe.{Decoder, DecodingFailure}

object TeamCityInstantParser {
  private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssZ")

  //this is a copy of the circe's decoder with a modified pattern
  @SuppressWarnings(Array(Wart.AsInstanceOf))
  implicit final val decodeInstant: Decoder[Instant] =
    Decoder.instance { c =>
      c.as[String] match {
        case Right(s) => try Right(Instant.from(dateTimeFormatter.parse(s))) catch {
          case _: DateTimeParseException => Left(DecodingFailure("Instant", c.history))
        }
        case l @ Left(_) => l.asInstanceOf[Decoder.Result[Instant]]
      }
    }
}
