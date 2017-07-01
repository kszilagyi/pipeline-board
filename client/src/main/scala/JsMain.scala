package com.kristofszilagyi

import Test._
import Wart._
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.TagOf
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, CallbackTo, CtorType, ScalaComponent}
import org.scalajs.dom
import org.scalajs.dom.html.Div
import slogging.{LoggerConfig, PrintLoggerFactory}

import scala.scalajs.js

final case class State(secondsElapsed: Long)

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

    val _ = Timer().renderIntoDOM(dom.document.getElementById("root"))
  }
}