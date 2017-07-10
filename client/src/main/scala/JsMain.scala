package com.kristofszilagyi


import Test._
import com.kristofszilagyi.shared.{AutowireApi, Wart}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder}
import japgolly.scalajs.react.vdom.TagOf
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, CallbackTo, ScalaComponent}
import org.scalajs.dom
import org.scalajs.dom.html.Div
import slogging.{LoggerConfig, PrintLoggerFactory}
import Wart._
import autowire.clientFutureCallable

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.typedarray.ArrayBuffer
import io.circe.parser.decode
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
final case class State(secondsElapsed: Long)


// client-side implementation, and call-site
@SuppressWarnings(Array(Wart.EitherProjectionPartial, Wart.Throw))
object MyClient extends autowire.Client[String, Decoder, Encoder]{
  def write[Result: Encoder](r: Result) = r.asJson.spaces2
  def read[Result: Decoder](p: String): Result = {
    val either = decode[Result](p)
    either.right.getOrElse(throw either.left.get) //stupid auto wire api, get server side version
  }

  def doCall(req: Request): Future[String] = {
    println(req.path.mkString("/"))
    /*dom.ext.Ajax.post(
      url = "/api/" + req.path.mkString("/"),
      headers = Map("Content-Type" -> "application/json")
    ).map(r => r.response.asInstanceOf[ArrayBuffer].toString)*/
    ???
  }


}


final class Backend($: BackendScope[Unit, State]) {
  @SuppressWarnings(Array(Var))
  private var interval: Option[js.timers.SetIntervalHandle] = None
  def tick: CallbackTo[Unit] =
    $.modState(s => State(s.secondsElapsed + 1))

  def start: Callback = Callback {
    interval = Some(js.timers.setInterval(1000)(tick.runNow()))
  }

  def clear: Callback = Callback {
    interval foreach js.timers.clearInterval
    interval = None
  }

  def render(s: State): TagOf[Div] =
    <.div("Seconds elapsed: ", s.secondsElapsed)
}

object Test {
  @SuppressWarnings(Array(Public))
  val Timer = ScalaComponent.builder[Unit]("Timer")
    .initialState(State(0))
    .renderBackend[Backend]
    .componentDidMount(_.backend.start)
    .componentWillUnmount(_.backend.clear)
    .build
}

object JsMain extends js.JSApp {
  LoggerConfig.factory = PrintLoggerFactory()

  def main(): Unit = {

    println("Application starting")
    MyClient[AutowireApi].dataFeed().call().foreach(println)

    val _ = Timer().renderIntoDOM(dom.document.getElementById("root"))
  }
}