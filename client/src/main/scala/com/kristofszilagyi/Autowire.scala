package com.kristofszilagyi

import autowire.clientFutureCallable
import com.kristofszilagyi.shared._
import io.circe.parser.decode
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder}
import org.scalajs.dom


import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js.typedarray.ArrayBuffer

// client-side implementation, and call-site
@SuppressWarnings(Array(Wart.EitherProjectionPartial, Wart.Throw))
class MyClient(implicit ec: ExecutionContext) extends autowire.Client[String, Decoder, Encoder]{
  def write[Result: Encoder](r: Result): String = r.asJson.spaces2
  def read[Result: Decoder](p: String): Result = {
    val either = decode[Result](p)
    either.right.getOrElse(throw either.left.get) //stupid auto wire api, get server side version
  }

  @SuppressWarnings(Array(Wart.AsInstanceOf))
  def doCall(req: Request): Future[String] = {
    dom.ext.Ajax.get(
      "/api/" + req.path.mkString("/")
    ).map(r => r.response.asInstanceOf[ArrayBuffer].toString)
  }
}


trait MockableAutowire {
  def dataFeed(): Future[BulkFetchResult]
}

final class RealAutowire(self: AutowireApi.Type)(implicit ec: ExecutionContext) extends MockableAutowire {
  def dataFeed(): Future[BulkFetchResult] = self.dataFeed().call()
}