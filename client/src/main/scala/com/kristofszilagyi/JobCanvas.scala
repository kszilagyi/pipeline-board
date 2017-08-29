package com.kristofszilagyi

import java.time.temporal.ChronoUnit
import java.time.{Duration, Instant, ZoneId, ZonedDateTime}

import com.kristofszilagyi.shared._
import japgolly.scalajs.react.vdom.{HtmlStyles, TagOf}
import japgolly.scalajs.react.vdom.svg_<^.{<, _}
import japgolly.scalajs.react.vdom
import japgolly.scalajs.react.{BackendScope, Callback, CallbackTo, ScalaComponent}
import org.scalajs.dom.html.Div

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.scalajs.js
import Wart._
import org.scalajs.dom.svg.SVG

final case class State(jenkinsState: BulkFetchResult)




final class JobCanvas($: BackendScope[Unit, State], timers: JsTimers, autowireApi: MockableAutowire)
                     (implicit ec: ExecutionContext) {
  @SuppressWarnings(Array(Var))
  private var interval: Option[js.timers.SetIntervalHandle] = None

  def tick: CallbackTo[Unit] = Callback.future {
    autowireApi.dataFeed().map { jenkinsState =>
      setState(jenkinsState)
    }
  }

  def setState(jenkinsState: BulkFetchResult): CallbackTo[Unit] = {
    Callback.log("setState") >> $.setState(State(jenkinsState))
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

  private def durationSince(i: Instant, sinceDuration: FiniteDuration): Duration = {
    //todo get proper zone id
    val dayAgo = ZonedDateTime.now().minusMinutes(sinceDuration.toMinutes).toInstant
    val duration = Duration.between(dayAgo, i)
    duration
  }

  def render(s: State): TagOf[SVG] = {
    val drawAreaWidth = 1700
    val labelEnd = 300

    val drawObjs = s.jenkinsState.results.zipWithIndex.flatMap { case (jobState, idx) =>
      val space = 30
      val y = idx * space + 15
      val label = <.text(
        ^.x := labelEnd,
        ^.y := y,
        ^.textAnchor := "end",
        ^.fill := "black",
        jobState.request.s
      )
      val rectangles = jobState.r match {
        case Left(err) =>
          List(<.text(
            ^.x := labelEnd,
            ^.y := y,
            ^.fill := "red",
            err.s
          ))
        case Right(runs) =>
          runs.flatMap(either => either match {
            case Right(run) =>
              val sinceDuration = 24.hours
              val dayMins = sinceDuration.toMinutes
              val startMin = durationSince(run.buildStart, sinceDuration).toMinutes
              val endMin = durationSince(run.buildFinish, sinceDuration).toMinutes
              //todo deal with more than a day longer
              //todo deal with partially inside
              val rectangle = if (startMin > 0) {
                val relativeStart = startMin.toDouble / dayMins
                val relativeEnd = endMin.toDouble / dayMins
                val relativeWidth = relativeEnd - relativeStart //todo assert if this is negative, also round up to >10?
                val start = relativeStart * drawAreaWidth
                val width = Math.max(1, relativeWidth * drawAreaWidth) //todo display these nicely, probably not really a problem
                //todo header, colors, hovering, zooming, horizontal lines, click
                Some(<.rect(
                  ^.x := (labelEnd + start).toInt,
                  ^.y := y - space/2,
                  ^.width := width.toInt,
                  ^.height := space/2
                ))
              } else
                None
              rectangle.toList
            case Left(value) =>
              //todo do sg
              None.toList
          })
      }
      label +: rectangles
    }
    //todo it doesn't work because instant is not a scalajs objet!
    val svgParams = drawObjs :+ (^.width := drawAreaWidth + labelEnd) :+ (^.height := 1000)
    <.svg(
      svgParams: _*
    )
  }
}

object Canvas {

  @SuppressWarnings(Array(Public))
  def jobCanvas(timers: JsTimers, autowire: MockableAutowire)(implicit ec: ExecutionContext) = {
    ScalaComponent.builder[Unit]("Timer")
      .initialState(State(BulkFetchResult(Seq.empty)))
      .backend(new JobCanvas(_, timers, autowire))
      .renderS(_.backend.render(_))
      .componentDidMount(_.backend.start)
      .componentWillUnmount(_.backend.clear)
      .build
  }

  implicit val ec: ExecutionContext = scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
  @SuppressWarnings(Array(Public))
  val JobCanvas = jobCanvas(RealJsTimers, new RealAutowire(new MyClient().apply[AutowireApi]))
}
