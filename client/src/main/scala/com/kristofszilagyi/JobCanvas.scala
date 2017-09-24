package com.kristofszilagyi

import java.time.format.{DateTimeFormatter, FormatStyle}
import java.time.temporal.ChronoUnit
import java.time.{Duration, Instant, ZoneId, ZonedDateTime}
import com.kristofszilagyi.shared.TypeSafeEqualsOps._
import com.kristofszilagyi.shared._
import japgolly.scalajs.react.vdom.{HtmlStyles, TagOf}
import japgolly.scalajs.react.vdom.svg_<^.{<, _}
import japgolly.scalajs.react.vdom
import japgolly.scalajs.react.{BackendScope, Callback, CallbackTo, ScalaComponent}
import org.scalajs.dom.html.Div
import ZonedDateTimeOps._
import InstantOps._
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.scalajs.js
import Wart._
import com.kristofszilagyi.shared.JenkinsBuildStatus.{Aborted, Building, Failed, Successful}
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
    interval = Some(timers.setInterval(30.seconds, {
      tick.runNow()
    }))
    tick.runNow()
  }

  def clear: Callback = Callback {
    interval foreach js.timers.clearInterval
    interval = None
  }

  def render(s: State): TagOf[SVG] = {
    val jobAreaWidthPx = 1600
    val labelEndPx = 300
    val space = 50
    val first = 50
    val spaceContentRatio = 0.75
    val drawingAreaDuration = 24.hours

    def textBaseLine(idx: Int): Int =  idx * space + first

    def backgroundBaseLine(idx: Int): Int = (textBaseLine(idx) - space * spaceContentRatio).toInt
    val colors = List("white", "yellow", "blue")
    val maxHorizontalBar = 5
    val now = ZonedDateTime.now()

    val verticleLines = (0 to maxHorizontalBar) flatMap { idx =>
      val x = jobAreaWidthPx / maxHorizontalBar * idx + labelEndPx
      val yStart = backgroundBaseLine(0)
      val yEnd = backgroundBaseLine(0) + s.jenkinsState.results.size * space + 10
      val timeOnBar = now - drawingAreaDuration + idx.toDouble / maxHorizontalBar * drawingAreaDuration
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
          timeOnBar.format(DateTimeFormatter.ofPattern("uuuu-MMM-dd HH:mm:ss"))
        )
      )
    }



    val drawObjs = s.jenkinsState.results.zipWithIndex.flatMap { case (jobState, idx) =>

      val label = <.text(
        ^.x := labelEndPx,
        ^.y := textBaseLine(idx),
        ^.textAnchor := "end",
        ^.fill := "black",
        jobState.request.s
      )
      val background = <.rect(
        ^.x := labelEndPx,
        ^.y := backgroundBaseLine(idx),
        ^.height := space,
        ^.width := jobAreaWidthPx,
        ^.fill := colors(idx % colors.size),
      )

      val drawingAreaBeginning = (now - drawingAreaDuration).toInstant
      val durationSinceDrawingAreaBeginning = now.toInstant - drawingAreaBeginning

      val jobRectangles = jobState.r match {
        case Left(err) =>
          List(<.text(
            ^.x := labelEndPx,
            ^.y := textBaseLine(idx),
            ^.fill := "red",
            err.s
          ))
        case Right(runs) =>
          runs.flatMap(either => either match {
            case Right(run) =>
              val startRelativeToDrawingAreaBeginning = run.buildStart - drawingAreaBeginning
              val endRelativeToDrawingAreaBeginning = run.buildFinish - drawingAreaBeginning
              //todo deal with more than a day longer
              //todo deal with partially inside
              val buildRectangle = if (startRelativeToDrawingAreaBeginning.toNanos > 0) {
                val relativeStartRatio = startRelativeToDrawingAreaBeginning / durationSinceDrawingAreaBeginning
                val relativeEndRatio = if (run.jenkinsBuildStatus ==== Building) {
                  1.0
                } else {
                  endRelativeToDrawingAreaBeginning / durationSinceDrawingAreaBeginning

                }
                val relativeWidthRatio = relativeEndRatio - relativeStartRatio //todo assert if this is negative, also round up to >10?
                val startPx = relativeStartRatio * jobAreaWidthPx
                val widthPx = Math.max(1, relativeWidthRatio * jobAreaWidthPx) //todo display these nicely, probably not really a problem
                //todo header, colors, hovering, zooming, horizontal lines, click
                val color = run.jenkinsBuildStatus match {
                  case Building => "black"
                  case Failed => "red"
                  case Successful => "green"
                  case Aborted => "pink"
                }
                Some(<.rect(
                  ^.x := (labelEndPx + startPx).toInt,
                  ^.y := (textBaseLine(idx) - space * spaceContentRatio * 0.75 ).toInt,
                  ^.width := widthPx.toInt,
                  ^.height := (space * spaceContentRatio).toInt,
                  ^.fill := color
                ))
              } else
                None
              buildRectangle.toList
            case Left(value) =>
              //todo do sg
              None.toList
          })
      }
      background +: label +: jobRectangles
    }
    //todo it doesn't work because instant is not a scalajs objet!
    val rightMargin = 100
    val svgParams = (drawObjs ++ verticleLines) :+ (^.width := jobAreaWidthPx + labelEndPx + rightMargin) :+ (^.height := 1000)
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