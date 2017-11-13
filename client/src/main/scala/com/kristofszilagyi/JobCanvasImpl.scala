package com.kristofszilagyi

import java.time.{Instant, ZoneId}

import com.kristofszilagyi.Canvas.queryJobWindowWidth
import com.kristofszilagyi.JobCanvasImpl.initialDuration
import com.kristofszilagyi.RenderUtils._
import com.kristofszilagyi.shared.InstantOps._
import com.kristofszilagyi.shared.MyStyles.{labelEndPx, rightMargin}
import com.kristofszilagyi.shared.TypeSafeEqualsOps._
import com.kristofszilagyi.shared.Wart._
import com.kristofszilagyi.shared._
import japgolly.scalajs.react.extra.{EventListener, OnUnmount}
import japgolly.scalajs.react.raw.{SyntheticMouseEvent, SyntheticWheelEvent}
import japgolly.scalajs.react.vdom._
import japgolly.scalajs.react.vdom.svg_<^.{<, _}
import japgolly.scalajs.react.{BackendScope, Callback, CallbackTo, ScalaComponent}
import org.scalajs.dom.Element
import org.scalajs.dom.raw.SVGElement
import slogging.LazyLogging

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration.Infinite
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.scalajs.js

final case class State(windowWidthPx: Int, ciState: ResultAndTime, drawingAreaDuration: FiniteDuration, durationIdx: Int,
                       endTime: Instant, mouseDownY: Option[Int], endTimeAtMouseDown: Instant, followTime: Boolean)

object JobCanvasImpl {
  val initialDuration: FiniteDuration = 24.hours
}

final class JobCanvasImpl($: BackendScope[Unit, State], timers: JsTimers, autowireApi: MockableAutowire)
                         (implicit ec: ExecutionContext) extends LazyLogging with OnUnmount {
  @SuppressWarnings(Array(Var))
  private var interval: Option[js.timers.SetIntervalHandle] = None

  def tick: CallbackTo[Unit] = $.modState{ s =>
    if (s.followTime) {
      s.copy(endTime = Instant.now)
    } else s
  } >> Callback.future {
    autowireApi.dataFeed().map { jenkinsState =>
      setJenkinsState(jenkinsState)
    }//todo show error when server is unreachable
  }

  def setJenkinsState(jenkinsState: ResultAndTime): CallbackTo[Unit] = {
    $.modState(s => {
      s.copy(ciState = jenkinsState)
    })
  }

  def adjustZoomLevel(delta: Double): CallbackTo[Unit] = {
    val validDurations = List(1.hour, 2.hours, 5.hours, 12.hours, initialDuration, 2.days, 5.days, 7.days, 14.days, 30.days, 60.days, 180.days, 365.days).sorted
    $.modState(s => {
      val idx = s.durationIdx
      val dir = math.signum(delta.toInt)

      val newIdx = (idx + dir).min(validDurations.length - 1).max(0)
      val newDuration = validDurations(newIdx)
      s.copy(drawingAreaDuration = newDuration, durationIdx = newIdx)
    })
  }


  def adjustEndTime(delta: Int): CallbackTo[Unit] = {
    $.modState(s => {
      val jobAreaWidthPx = s.windowWidthPx
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
      s.copy(endTime = validatedEnd, followTime = false)
    })
  }

  def start: Callback = Callback {
    interval = Some(timers.setInterval(30.seconds, {
      tick.runNow()
    })) } >> tick >> resized


  def clear: Callback = Callback {
    interval foreach js.timers.clearInterval
    interval = None
  }

  def resized: Callback = {
    $.modState(s => s.copy(windowWidthPx = Math.max(queryJobWindowWidth(), labelEndPx + rightMargin + 100)))
  }

  def render(s: State): TagOf[Element] = {
    val windowWidthPx = s.windowWidthPx
    val jobAreaWidthPx = windowWidthPx - labelEndPx - rightMargin
    val space = 30
    val generalMargin = 10
    import s.drawingAreaDuration

    def textMiddleLine(idx: Int): Int = (backgroundBaseLine(idx) + backgroundBaseLine(idx + 1)) / 2

    def backgroundBaseLine(idx: Int): Int = idx * space
    val colors = List("black", "darkslategrey")

    s.ciState.cachedResult.maybe.map { ciState =>
      //todo handle if jobs are not fecthed yet
      //todo handle if data is stale

      val jobArea = JobArea(jobAreaWidthPx, s.endTime, drawingAreaDuration)
      val topOfVerticalLinesYPx = backgroundBaseLine(0)
      val bottomOfVerticalLinesYPx = backgroundBaseLine(0) + ciState.results.size * space + generalMargin
      val timestampTextYPx = bottomOfVerticalLinesYPx + generalMargin

      val verticleLines = moveTo(
        x = labelEndPx,
        elements = verticalLines(topOfVerticalLinesYPx = topOfVerticalLinesYPx, bottomOfVerticalLinesYPx = bottomOfVerticalLinesYPx,
          timestampTextYPx = timestampTextYPx, jobArea, timeZone = ZoneId.systemDefault())
      )
      //todo show warning if some of the queries failed
      val labels = ciState.results.zipWithIndex.map { case (jobState, idx) =>
        val numberOfErrors = jobState.r.getOrElse(Seq.empty).map(_.isLeft).count(_ ==== true)
        val warningMsg = if (numberOfErrors > 0) {
          "\u26A0 "
        } else ""


        <.text(
          ^.x := labelEndPx - generalMargin/2,
          ^.y := textMiddleLine(idx),
          ^.textAnchor := textAnchorEnd,
          dominantBaseline := dominantBaselineCentral,
          <.tspan(
            ^.fill := "red",
            <.title(s"$numberOfErrors build was not shown due to errors. Please check out the JavaScript console for details."),
            warningMsg,
          ),
          a(
            href := jobState.request.urls.userRoot.u.rawString,
            target := "_blank",
            <.tspan(
              textDecoration := "underline",
              ^.fill := "black",
              jobState.request.name.s
            )
          )
        )

      }

      //todo add links to labels and builds
      val drawObjs = ciState.results.zipWithIndex.map { case (jobState, idx) =>
        val oneStrip = nestAt(
          x = labelEndPx,
          y = backgroundBaseLine(idx),
          elements = List(
            strip(
              jobAreaWidthPx = jobAreaWidthPx,
              stripHeight = space,
              colors(idx % colors.size),
              jobRectanges(jobState = jobState, jobArea = jobArea, rectangleHeight = (space * 0.75).toInt,
                stripHeight = space)
            )
          )
        )
        oneStrip
      }
      val periodText = <.text(^.x := labelEndPx + jobAreaWidthPx, ^.y := backgroundBaseLine(-1),
        s"${s.drawingAreaDuration}", ^.textAnchor := textAnchorEnd, dominantBaseline := "text-before-edge")
      val wheelListener = html_<^.^.onWheel ==> handleWheel
      val dragListeners = List(html_<^.^.onMouseDown ==> handleDown,
        html_<^.^.onMouseMove ==> handleMove,
        html_<^.^.onMouseUp ==> handleUp,
      )

      val groupedDrawObjs = <.g((drawObjs ++ dragListeners) :+ wheelListener: _*)
      val checkboxId = "follow"
      val input = <.foreignObject(
        ^.x := labelEndPx,
        ^.y := backgroundBaseLine(-1),
        ^.width := 100, //single line
        //todo replace this with SVG checkbox, this is quite hard to align
        html_<^.<div(
          html_<^.< input(
            html_<^.^.id := checkboxId,
            html_<^.^.`type` := "checkbox",
            html_<^.^.checked := s.followTime,
            html_<^.^.onChange --> $.modState { s =>
              val follow = !s.followTime
              val endTime = if (follow) Instant.now else s.endTime
              s.copy(followTime = follow, endTime = endTime)
            }
          ),
          html_<^.<.label(html_<^.^.`for` := checkboxId, "Follow")
        )
      )
      val offsetOnPageY = 50
      val svgParams = List(
        moveTo(y = offsetOnPageY, elements = List(groupedDrawObjs, verticleLines, periodText) ++ labels :+ input),
        ^.width := windowWidthPx, ^.height := timestampTextYPx + offsetOnPageY + 10 //+10 to let the bottom of the text in
      )

      <.svg(
        svgParams: _*
      )
    }.getOrElse(html_<^.<.p("No data yet"))
  }
  def handleWheel(e: SyntheticWheelEvent[SVGElement]): CallbackTo[Unit] = {
   e.stopPropagationCB >> e.preventDefaultCB >>
      adjustZoomLevel(e.deltaY)
  }

  //todo make these only work on the drawing area
  def handleDown(e: SyntheticMouseEvent[SVGElement]): CallbackTo[Unit] = {
    val x = e.clientX.toInt //this is mutable, so need to get it
    e.stopPropagationCB >> e.preventDefaultCB >> //todo enable copying of text
      Callback { org.scalajs.dom.document.body.style.cursor = "all-scroll" } >>
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
    Callback { org.scalajs.dom.document.body.style.cursor = "auto" } >>
      $.modState(s => s.copy(mouseDownY = None))
  }
}
//todo I have no sense of unit: there should be different horizontal lines with different color
// which signal minutes, hours, days, weeks. months
object Canvas {

  def queryJobWindowWidth(): Int = {
    org.scalajs.dom.document.body.clientWidth
  }

  @SuppressWarnings(Array(Public))
  def jobCanvas(timers: JsTimers, autowire: MockableAutowire)(implicit ec: ExecutionContext) = {
    ScalaComponent.builder[Unit]("Timer")
      .initialState(State(queryJobWindowWidth(), ResultAndTime(CachedResult(None), Instant.now), drawingAreaDuration = initialDuration, durationIdx = 4, //:(
        Instant.now(), mouseDownY = None, endTimeAtMouseDown = Instant.now, followTime = true))
      .backend(new JobCanvasImpl(_, timers, autowire))
      .renderS(_.backend.render(_))
      .componentDidMount(_.backend.start)
      .componentWillUnmount(_.backend.clear)
      .configure(
        EventListener.install("resize", _.backend.resized, _ => org.scalajs.dom.window)
      )
      .build
  }

  implicit val ec: ExecutionContext = scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
  @SuppressWarnings(Array(Public))
  val JobCanvas = jobCanvas(RealJsTimers, new RealAutowire(new MyClient().apply[AutowireApi]))
}
