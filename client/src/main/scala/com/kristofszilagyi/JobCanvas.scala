package com.kristofszilagyi

import java.time.{Instant, ZoneId}

import com.kristofszilagyi.RenderUtils._
import com.kristofszilagyi.shared.InstantOps._
import com.kristofszilagyi.shared.Wart._
import com.kristofszilagyi.shared._
import japgolly.scalajs.react.raw.{SyntheticMouseEvent, SyntheticWheelEvent}
import japgolly.scalajs.react.vdom._
import japgolly.scalajs.react.vdom.svg_<^.{<, _}
import japgolly.scalajs.react.{BackendScope, Callback, CallbackTo, ScalaComponent}
import org.scalajs.dom.raw.{HTMLElement, SVGElement}
import slogging.LazyLogging
import TypeSafeEqualsOps._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration.Infinite
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.scalajs.js

final case class State(ciState: ResultAndTime, drawingAreaDuration: FiniteDuration,
                       endTime: Instant, mouseDownY: Option[Int], endTimeAtMouseDown: Instant, followTime: Boolean)

final class JobCanvas($: BackendScope[Unit, State], timers: JsTimers, autowireApi: MockableAutowire)
                     (implicit ec: ExecutionContext) extends LazyLogging {
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
      s.copy(endTime = validatedEnd, followTime = false)
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

  def render(s: State): TagOf[HTMLElement] = {

    val labelEndPx = 300
    val space = 50
    val first = 50
    val spaceContentRatio = 0.75
    import s.drawingAreaDuration

    def textBaseLine(idx: Int): Int = idx * space + first

    def backgroundBaseLine(idx: Int): Int = (textBaseLine(idx) - space * spaceContentRatio).toInt
    val colors = List("black", "darkslategrey")

    s.ciState.cachedResult.maybe.map { ciState =>
      //todo handle if jobs are not fecthed yet
      //todo handle if data is stale

      val jobArea = JobArea(jobAreaWidthPx, s.endTime, drawingAreaDuration)
      val verticleLines = moveTo(
        x = labelEndPx,
        elements = verticalLines(backgroundBaseLine = backgroundBaseLine, numberOfJobs = ciState.results.size,
          jobHeight = space, jobArea, timeZone = ZoneId.systemDefault())
      )
      //todo show warning if some of the queries failed
      val labels = ciState.results.zipWithIndex.map { case (jobState, idx) =>
        val numberOfErrors = jobState.r.getOrElse(Seq.empty).map(_.isLeft).count(_ ==== true)
        val warningMsg = if (numberOfErrors > 0) {
          "\u26A0 "
        } else ""


        <.text(
          ^.x := labelEndPx,
          ^.y := textBaseLine(idx),
          ^.textAnchor := "end",
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
      val rightMargin = 100
      val wheelListener = html_<^.^.onWheel ==> handleWheel
      val dragListeners = List(html_<^.^.onMouseDown ==> handleDown,
        html_<^.^.onMouseMove ==> handleMove,
        html_<^.^.onMouseUp ==> handleUp,
      )

      val groupedDrawObjs = <.g((drawObjs ++ dragListeners) :+ wheelListener: _*)
      val svgParams = (labels :+ (^.width := jobAreaWidthPx + labelEndPx + rightMargin) :+
        (^.height := 1000) :+ groupedDrawObjs) :+ verticleLines
      val checkboxId = "follow"
      html_<^.<.div(
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
        html_<^.<.label(html_<^.^.`for` := checkboxId, "Follow"),
        <.svg(
          svgParams: _*
        )
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

  @SuppressWarnings(Array(Public))
  def jobCanvas(timers: JsTimers, autowire: MockableAutowire)(implicit ec: ExecutionContext) = {
    ScalaComponent.builder[Unit]("Timer")
      .initialState(State(ResultAndTime(CachedResult(None), Instant.now), drawingAreaDuration = 1.days,
        Instant.now(), mouseDownY = None, endTimeAtMouseDown = Instant.now, followTime = true))
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
