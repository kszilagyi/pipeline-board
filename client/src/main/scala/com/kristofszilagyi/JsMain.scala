package com.kristofszilagyi

import autowire.{ClientProxy, clientFutureCallable}
import com.kristofszilagyi.Test._
import com.kristofszilagyi.shared.Wart._
import com.kristofszilagyi.shared.{AutowireApi, FetchResult, Url, Wart}
import io.circe.parser.decode
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder}
import japgolly.scalajs.react.vdom.TagOf
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, CallbackTo, ScalaComponent}
import org.scalajs.dom
import org.scalajs.dom.XMLHttpRequest
import org.scalajs.dom.html.Div
import slogging.{LoggerConfig, PrintLoggerFactory}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.scalajs.js
import scala.scalajs.js.timers.SetIntervalHandle
import scala.scalajs.js.typedarray.ArrayBuffer

final case class State(s: String)


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

trait JsTimers {
  def setInterval(interval: FiniteDuration, body: => Unit): SetIntervalHandle
  def clearInterval(handle: SetIntervalHandle): Unit
}

object RealJsTimers extends JsTimers {
  def setInterval(interval: FiniteDuration, body:  => Unit): SetIntervalHandle = {
    js.timers.setInterval(interval)(body)
  }

  def clearInterval(handle: SetIntervalHandle): Unit = {
    js.timers.clearInterval(handle)
  }
}

trait MockableAutowire {
  def dataFeed(): Future[FetchResult]
}

final class RealAutowire(self: AutowireApi.Type)(implicit ec: ExecutionContext) extends MockableAutowire {
  def dataFeed(): Future[FetchResult] = self.dataFeed().call()
}

final class Backend($: BackendScope[Unit, State], timers: JsTimers, autowireApi: MockableAutowire)
                   (implicit ec: ExecutionContext){
  @SuppressWarnings(Array(Var))
  private var interval: Option[js.timers.SetIntervalHandle] = None
  def tick: CallbackTo[Unit] = Callback.future {
    autowireApi.dataFeed().map { s =>
      setString(s.toString)
    }
  }

  def setString(s: String): CallbackTo[Unit] = {
    Callback.log("setString") >> $.setState(State(s))
  }
  def start: Callback = Callback {
    interval = Some(timers.setInterval(1.seconds, {
      tick.runNow()
    }))
  }

  def clear: Callback = Callback {
    interval foreach js.timers.clearInterval
    interval = None
  }

  def render(s: State): TagOf[Div] =
    <.div("Seconds elapsed: ", s.s)}

object Test {

  @SuppressWarnings(Array(Public))
  def timer(timers: JsTimers, autowire: MockableAutowire)(implicit ec: ExecutionContext) = {
    ScalaComponent.builder[Unit]("Timer")
      .initialState(State("nothing"))
      .backend(new Backend(_, timers, autowire))
      .renderS(_.backend.render(_))
      .componentDidMount(_.backend.start)
      .componentWillUnmount(_.backend.clear)
      .build
  }

  implicit val ec: ExecutionContext = scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
  @SuppressWarnings(Array(Public))
  val Timer = timer(RealJsTimers, new RealAutowire(new MyClient().apply[AutowireApi]))
}

object JsMain extends js.JSApp {
  LoggerConfig.factory = PrintLoggerFactory()

  def main(): Unit = {

    println("Application starting")

    val _ = Timer().renderIntoDOM(dom.document.getElementById("root"))
  }
}