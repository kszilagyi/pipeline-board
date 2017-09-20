package com.kristofszilagyi.shared

import java.io.{PrintWriter, StringWriter}

import io.circe.generic.JsonCodec

object SThrowable {
  @SuppressWarnings(Array(Wart.Recursion))
  def from(t: Throwable): SThrowable = {
    val stackTraceWriter = new StringWriter
    t.printStackTrace(new PrintWriter(stackTraceWriter))
    new SThrowable(t.getMessage, stackTraceWriter.toString, Option(t.getCause).map(SThrowable.from)) //todo fix recursion?
  }
}
/**
  * S stands for Serializable
  */
@JsonCodec final case class SThrowable(message: String, stackTrace: String, cause: Option[SThrowable])
