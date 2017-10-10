package com.kristofszilagyi.controllers

import com.kristofszilagyi.shared.{AutowireApi, Wart}
import io.circe.parser.decode
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder}
import com.kristofszilagyi.controllers.AutowireServer.throwEither

import scala.concurrent.ExecutionContext

object AutowireServer {
  //mandated by the autowire api :(
  @SuppressWarnings(Array(Wart.EitherProjectionPartial, Wart.Throw))
  def throwEither[A](either: Either[io.circe.Error, A]): A = {
    either.right.getOrElse(throw either.left.get)
  }
}

class AutowireServer(impl: AutowireApiImpl)(implicit ec: ExecutionContext) extends autowire.Server[String, Decoder, Encoder]{
  def write[R: Encoder](r: R): String = r.asJson.spaces2
  def read[R: Decoder](p: String): R = {
    val either = decode[R](p)
    throwEither(either)
  }

  val routes: Router = route[AutowireApi](impl)
}