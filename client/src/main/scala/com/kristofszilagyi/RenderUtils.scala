package com.kristofszilagyi

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId}

import com.kristofszilagyi.shared.BuildStatus.{Aborted, Building, Failed, Successful}
import com.kristofszilagyi.shared.InstantOps._
import com.kristofszilagyi.shared.TypeSafeEqualsOps._
import com.kristofszilagyi.shared.ZonedDateTimeOps._
import com.kristofszilagyi.shared.{JobDetails, MyStyles, Wart}
import japgolly.scalajs.react.vdom.PackageBase.VdomAttr
import japgolly.scalajs.react.vdom.svg_<^._
import japgolly.scalajs.react.vdom.{SvgTagOf => _, TagMod => _, _}
import org.scalajs.dom.raw._
import org.scalajs.dom.svg.{A, G, SVG}

import scala.collection.immutable
import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scalacss.ScalaCssReact._

@SuppressWarnings(Array(Wart.Overloading))
object RenderUtils {

  def alignmentBaseline: VdomAttr[Any] = VdomAttr("alignmentBaseline")

  def className: VdomAttr[Any] = VdomAttr("className")

  def target: VdomAttr[Any] = VdomAttr("target")

  def a: SvgTagOf[A] = SvgTagOf[A]("a")

  def href: VdomAttr[Any] = VdomAttr("href")

  def textDecoration = VdomAttr("textDecoration")

  def animation = VdomAttr("animation")

  final case class JobArea(widthPx: Int, endTime: Instant, drawingAreaDuration: FiniteDuration) {
    def startTime: Instant = endTime - drawingAreaDuration
    def length: FiniteDuration = endTime - startTime
  }
  @SuppressWarnings(Array(Wart.DefaultArguments))
  def nestAt(x: Int = 0, y: Int = 0, elements: Seq[TagMod]): TagOf[SVG] = {
    <.svg(elements ++ List(^.x := x, ^.y := y): _*)
  }

  def nestAt(x: Int, y: Int, elements: TagMod): TagOf[SVG] = {
    nestAt(x, y, List(elements))
  }

  @SuppressWarnings(Array(Wart.DefaultArguments))
  def moveTo(x: Int = 0, y: Int = 0, elements: Seq[TagMod]): TagOf[G] = {
    <.g(elements ++ List(^.transform := s"translate($x, $y)"): _*)
  }

  def moveTo(x: Int, y: Int, elements: TagMod): TagOf[G] = {
    moveTo(x, y, List(elements))
  }

  def verticalLines(backgroundBaseLine: Int => Int, numberOfJobs: Int, jobHeight: Int,
                    jobArea: JobArea, timeZone: ZoneId): immutable.Seq[TagOf[SVGElement]] = {
    val maxHorizontalBar = 5
    (0 to maxHorizontalBar) flatMap { idx =>
      val x = jobArea.widthPx / maxHorizontalBar * idx
      val yStart = backgroundBaseLine(0)
      val yEnd = backgroundBaseLine(0) + numberOfJobs * jobHeight + 10
      val timeOnBar = jobArea.endTime.atZone(timeZone) - jobArea.drawingAreaDuration + idx.toDouble / maxHorizontalBar * jobArea.drawingAreaDuration
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
  }

  def strip(jobAreaWidthPx: Int, stripHeight: Int, color: String, elementsInside: Seq[TagMod]): TagOf[SVG] = {

    val background = <.rect(
      ^.height := stripHeight,
      ^.width := jobAreaWidthPx,
      ^.fill := color,
    )
    <.svg(
      List(
        ^.height := stripHeight,
        ^.width := jobAreaWidthPx,
        background
      ) ++ elementsInside: _*
    )
  }

  def jobRectanges(jobState: JobDetails, jobArea: JobArea, rectangleHeight: Int, stripHeight: Int): Seq[TagOf[SVGElement]] = {
    jobState.r match {
      case Left(err) =>
        List(<.text(
          ^.fill := "red",
          alignmentBaseline := "middle",
          ^.y := stripHeight/2,
          err.s
        ))
      case Right(runs) =>
        runs.flatMap(either => either match {
          case Right(run) =>
            val startRelativeToDrawingAreaBeginning = (run.buildStart - jobArea.startTime).max(0.seconds)
            val endRelativeToDrawingAreaBeginning = (run.buildFinish - jobArea.startTime).min(jobArea.length)
            //todo deal with more than a day longer
            //todo deal with partially inside
            val buildRectangle = if (endRelativeToDrawingAreaBeginning.toNanos > 0 &&
              startRelativeToDrawingAreaBeginning < jobArea.length) {

              val relativeStartRatio = startRelativeToDrawingAreaBeginning / jobArea.length
              val relativeEndRatio = if (run.buildStatus ==== Building) {
                1.0
              } else {
                endRelativeToDrawingAreaBeginning / jobArea.length
              }
              val relativeWidthRatio = relativeEndRatio - relativeStartRatio //todo assert if this is negative, also round up to >10?
              val startPx = relativeStartRatio * jobArea.widthPx
              //todo this will go out of the drawing area, fix
              val widthPx = Math.max(4, relativeWidthRatio * jobArea.widthPx) //todo display these nicely, probably not really a problem
              //todo header, colors, hovering, zooming, horizontal lines, click

              val style: List[TagMod] = run.buildStatus match {
                case Building => List(MyStyles.building)
                case Failed => List(MyStyles.failed)
                case Successful => List(MyStyles.success)
                case Aborted => List(MyStyles.aborted)
              }

              val nonStyle = List(
                ^.x := startPx.toInt,
                ^.y := (stripHeight - rectangleHeight)/2,
                ^.width := widthPx.toInt,
                ^.height := rectangleHeight.toInt,
                className := s"build_rect",
                //todo add length
                //todo not have ended when building
                //todo replace this with jQuery or sg similar and make it pop up immediately not after delay and not browser dependent way
                <.title(s"Id: ${run.buildNumber.i}\nStart: ${run.buildStart}\nFinish: ${run.buildFinish}\nStatus: ${run.buildStatus}")
              )
              Some(<.rect(nonStyle ++ style: _*))
            } else
              None
            buildRectangle.toList
          case Left(value) =>
            //todo do sg
            None.toList
        })
    }
  }
}
