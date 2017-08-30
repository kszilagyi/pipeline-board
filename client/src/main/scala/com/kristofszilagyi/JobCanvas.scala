package com.kristofszilagyi

import java.time.format.{DateTimeFormatter, FormatStyle}
import java.time.temporal.ChronoUnit
import java.time.{Duration, Instant, ZoneId, ZonedDateTime}

import com.kristofszilagyi.shared._
import japgolly.scalajs.react.vdom.{HtmlStyles, TagOf}
import japgolly.scalajs.react.vdom.svg_<^.{<, _}
import japgolly.scalajs.react.vdom
import japgolly.scalajs.react.{BackendScope, Callback, CallbackTo, ScalaComponent}
import org.scalajs.dom.html.Div
import ZonedDateTimeOps._

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
    val dayAgo = (ZonedDateTime.now() - sinceDuration).toInstant
    val duration = Duration.between(dayAgo, i)
    duration
  }

  def render(s: State): TagOf[SVG] = {
    val drawAreaWidth = 1700
    val labelEnd = 300
    val space = 50
    val first = 50
    val spaceContentRatio = 0.75
    val sinceDuration = 24.hours

    def textBaseLine(idx: Int): Int =  idx * space + first

    def backgroundBaseLine(idx: Int): Int = (textBaseLine(idx) - space * spaceContentRatio).toInt
    val colors = List("red", "green", "blue")
    val maxHorizontalBar = 5
    val horizontalBars = (0 to maxHorizontalBar) flatMap { idx =>
      val x = drawAreaWidth / maxHorizontalBar * idx + labelEnd
      val yStart = backgroundBaseLine(0)
      val yEnd = backgroundBaseLine(0) + s.jenkinsState.results.size * space + 10
      val timeOnBar = ZonedDateTime.now() - sinceDuration + idx.toDouble / maxHorizontalBar * sinceDuration
      List(
        <.line(
          ^.x1 := x,
          ^.y1 := yStart,
          ^.x2 := x,
          ^.y2 := yEnd,
          ^.strokeWidth := "1",
          ^.stroke := "grey"
        ),
        <.text(
          ^.x := x,
          ^.y := yEnd + 10,
          ^.textAnchor := "middle",
          timeOnBar.format(DateTimeFormatter.ofPattern("uuuu-MMM-dd HH:mm"))
        )
      )
    }



    val drawObjs = s.jenkinsState.results.zipWithIndex.flatMap { case (jobState, idx) =>

      val label = <.text(
        ^.x := labelEnd,
        ^.y := textBaseLine(idx),
        ^.textAnchor := "end",
        ^.fill := "black",
        jobState.request.s
      )
      val background = <.rect(
        ^.x := labelEnd,
        ^.y := backgroundBaseLine(idx),
        ^.height := space,
        ^.width := drawAreaWidth,
        ^.fill := colors(idx % colors.size),
      )

      val rectangles = jobState.r match {
        case Left(err) =>
          List(<.text(
            ^.x := labelEnd,
            ^.y := textBaseLine(idx),
            ^.fill := "red",
            err.s
          ))
        case Right(runs) =>
          runs.flatMap(either => either match {
            case Right(run) =>
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
                  ^.y := (textBaseLine(idx) - space * spaceContentRatio * 0.75 ).toInt,
                  ^.width := width.toInt,
                  ^.height := (space * spaceContentRatio).toInt
                ))
              } else
                None
              rectangle.toList
            case Left(value) =>
              //todo do sg
              None.toList
          })
      }
      background +: label +: rectangles
    }
    //todo it doesn't work because instant is not a scalajs objet!
    val svgParams = (drawObjs ++ horizontalBars) :+ (^.width := drawAreaWidth + labelEnd) :+ (^.height := 1000)
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
