package com.kristofszilagyi

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId}

import com.kristofszilagyi.Canvas.className
import com.kristofszilagyi.shared.JenkinsBuildStatus.Building
import com.kristofszilagyi.shared.TypeSafeEqualsOps._
import com.kristofszilagyi.shared.Wart._
import com.kristofszilagyi.shared.ZonedDateTimeOps._
import com.kristofszilagyi.shared._
import japgolly.scalajs.react.raw.{SyntheticDragEvent, SyntheticMouseEvent, SyntheticWheelEvent}
import japgolly.scalajs.react.vdom.PackageBase.VdomAttr
import japgolly.scalajs.react.vdom.TagOf
import japgolly.scalajs.react.vdom.svg_<^.{<, _}
import japgolly.scalajs.react.{BackendScope, Callback, CallbackTo, ScalaComponent, vdom}
import org.scalajs.dom.raw.SVGElement
import org.scalajs.dom.svg.SVG
import InstantOps._
import slogging.LazyLogging

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration.Infinite
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.scalajs.js

final case class State(jenkinsState: BulkFetchResult, drawingAreaDuration: FiniteDuration,
                       endTime: Instant, mouseDownY: Option[Int], endTimeAtMouseDown: Instant)

final class JobCanvas($: BackendScope[Unit, State], timers: JsTimers, autowireApi: MockableAutowire)
                     (implicit ec: ExecutionContext) extends LazyLogging {
  @SuppressWarnings(Array(Var))
  private var interval: Option[js.timers.SetIntervalHandle] = None

  def tick: CallbackTo[Unit] = Callback.future {
    autowireApi.dataFeed().map { jenkinsState =>
      setJenkinsState(jenkinsState)
    }
  }

  def setJenkinsState(jenkinsState: BulkFetchResult): CallbackTo[Unit] = {
    $.modState(s => {
      s.copy(jenkinsState = jenkinsState)
    })
  }

  def adjustZoomLevel(delta: Double): CallbackTo[Unit] = {
    $.modState(s => {
      val to10Percent = 1 + Math.abs(delta) / 530
      val signedTo10Percent = if (delta > 0) {
        to10Percent
      } else {
        1 / to10Percent
      }
      val maxDuration = 365.days
      val newDuration = s.drawingAreaDuration * signedTo10Percent match {
        case _: Infinite => maxDuration
        case f: FiniteDuration => f
      }
      s.copy(drawingAreaDuration = 1.hour.max(newDuration).min(maxDuration))
    })
  }

  private val jobAreaWidthPx = 1600

  def adjustEndTime(delta: Int): CallbackTo[Unit] = {
    $.modState(s => {
      println(delta)
      val timeDelta = (delta.toDouble / jobAreaWidthPx) * s.drawingAreaDuration match {
        case _: Infinite => 0.seconds
        case f: FiniteDuration => f
      }
      val validatedEnd = {
        val newEnd = s.endTimeAtMouseDown + timeDelta
        val now = Instant.now
        if (newEnd.isAfter(now)) {
          now
        } else {
          newEnd
        }
      }
      s.copy(endTime = validatedEnd)
    })
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

    val labelEndPx = 300
    val space = 50
    val first = 50
    val spaceContentRatio = 0.75
    import s.drawingAreaDuration

    def textBaseLine(idx: Int): Int =  idx * space + first

    def backgroundBaseLine(idx: Int): Int = (textBaseLine(idx) - space * spaceContentRatio).toInt
    val colors = List("white", "yellow", "blue")
    val maxHorizontalBar = 5
    //val now = ZonedDateTime.now()

    val verticleLines = (0 to maxHorizontalBar) flatMap { idx =>
      val x = jobAreaWidthPx / maxHorizontalBar * idx + labelEndPx
      val yStart = backgroundBaseLine(0)
      val yEnd = backgroundBaseLine(0) + s.jenkinsState.results.size * space + 10
      val timeOnBar = s.endTime.atZone(ZoneId.systemDefault()) - drawingAreaDuration + idx.toDouble / maxHorizontalBar * drawingAreaDuration
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

      val drawingAreaBeginning = s.endTime - drawingAreaDuration
      val durationSinceDrawingAreaBeginning = s.endTime - drawingAreaBeginning

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
                val widthPx = Math.max(4, relativeWidthRatio * jobAreaWidthPx) //todo display these nicely, probably not really a problem
                //todo header, colors, hovering, zooming, horizontal lines, click

                Some(<.rect(
                  ^.x := (labelEndPx + startPx).toInt,
                  ^.y := (textBaseLine(idx) - space * spaceContentRatio * 0.75).toInt,
                  ^.width := widthPx.toInt,
                  ^.height := (space * spaceContentRatio).toInt,
                  className := s"${run.jenkinsBuildStatus.entryName.toLowerCase} build_rect",
                  //todo add length
                  //todo not have ended when building
                  //todo replace this with jQuery or sg similar and make it pop up immediately not after delay and not browser dependent way
                  <.title(s"Id: ${run.buildNumber.i}\nStart: ${run.buildStart}\nFinish: ${run.buildFinish}\nStatus: ${run.jenkinsBuildStatus}")
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
    val rightMargin = 100
    val wheelListener = vdom.html_<^.^.onWheel ==> handleSubmit
    val dragListeners = List(vdom.html_<^.^.onMouseDown ==> handleDown,
      vdom.html_<^.^.onMouseMove ==> handleMove,
      vdom.html_<^.^.onMouseUp ==> handleUp,
    )
    val svgParams = (drawObjs ++ verticleLines ++ dragListeners) :+ (^.width := jobAreaWidthPx + labelEndPx + rightMargin) :+
      (^.height := 1000) :+ wheelListener
    <.svg(
      svgParams: _*
    )
  }
  def handleSubmit(e: SyntheticWheelEvent[SVGElement]): CallbackTo[Unit] = {
   e.stopPropagationCB >> e.preventDefaultCB >>
      adjustZoomLevel(e.deltaY)
  }

  def handleDown(e: SyntheticMouseEvent[SVGElement]): CallbackTo[Unit] = {
    val x = e.clientX.toInt //this is mutable, so need to get it
    e.stopPropagationCB >> e.preventDefaultCB >> //todo enable copying of text
    $.modState(s => s.copy(mouseDownY = Some(x), endTimeAtMouseDown = s.endTime))
  }

  def handleMove(e: SyntheticMouseEvent[SVGElement]): CallbackTo[Unit] = {
    val x = e.clientX.toInt //this is mutable, so need to get it
    $.state.flatMap { s =>
      s.mouseDownY.map(_ - x) match {
        case Some(deltaX) => adjustEndTime(deltaX)
        case None => CallbackTo(())
      }
    }

  }

  def handleUp(e: SyntheticMouseEvent[SVGElement]): CallbackTo[Unit] = {
    val _ = e
    $.modState(s => s.copy(mouseDownY = None))
  }
}

object Canvas {

  def className = VdomAttr("className")

  @SuppressWarnings(Array(Public))
  def jobCanvas(timers: JsTimers, autowire: MockableAutowire)(implicit ec: ExecutionContext) = {
    ScalaComponent.builder[Unit]("Timer")
      .initialState(State(BulkFetchResult(Seq.empty), drawingAreaDuration = 1.days,
        Instant.now(), mouseDownY = None, endTimeAtMouseDown = Instant.now))
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
