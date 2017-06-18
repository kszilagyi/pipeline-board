import Test._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ScalaComponent}
import org.scalajs.dom
import slogging.{LoggerConfig, PrintLoggerFactory}

import scala.scalajs.js

case class State(secondsElapsed: Long)

class Backend($: BackendScope[Unit, State]) {
  var interval: Option[js.timers.SetIntervalHandle] = None
  def tick =
    $.modState(s => State(s.secondsElapsed + 1))

  def start = Callback {
    interval = Some(js.timers.setInterval(1000)(tick.runNow()))
  }

  def clear = Callback {
    interval foreach js.timers.clearInterval
    interval = None
  }

  def render(s: State) =
    <.div("Seconds elapsed: ", s.secondsElapsed)
}

object Test {
  val Timer = ScalaComponent.builder[Unit]("Timer")
    .initialState(State(0))
    .renderBackend[Backend]
    .componentDidMount(_.backend.start)
    .componentWillUnmount(_.backend.clear)
    .build
}

object Main extends js.JSApp {
  LoggerConfig.factory = PrintLoggerFactory()

  def main(): Unit = {

    println("Application starting")

    val _ = Timer().renderIntoDOM(dom.document.getElementById("root"))
  }
}