package com.kristofszilagyi.pipelineboard.shared

import autowire.ClientProxy
import io.circe.{Decoder, Encoder}

import scala.concurrent.Future

object AutowireApi {
  type Type = ClientProxy[AutowireApi, String, Decoder, Encoder]
}

trait AutowireApi{
  def dataFeed(): Future[ResultAndTime]
}