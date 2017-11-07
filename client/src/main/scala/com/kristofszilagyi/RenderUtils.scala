package com.kristofszilagyi

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId}

import com.kristofszilagyi.shared.BuildStatus.{Aborted, Building, Created, Failed, Pending, Successful, Unstable}
import com.kristofszilagyi.shared.InstantOps._
import com.kristofszilagyi.shared.ZonedDateTimeOps._
import com.kristofszilagyi.shared.{JobDetails, MyStyles, Wart}
import japgolly.scalajs.react.vdom.PackageBase.VdomAttr
import japgolly.scalajs.react.vdom.svg_<^._
import japgolly.scalajs.react.vdom.{SvgTagOf => _, TagMod => _, _}
import org.scalajs.dom.raw._
import org.scalajs.dom.svg.{A, G, SVG}
import slogging.LazyLogging

import scala.collection.immutable
import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scalacss.ScalaCssReact._

@SuppressWarnings(Array(Wart.Overloading))
object RenderUtils extends LazyLogging {

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

  def verticalLines(topOfVerticalLinesYPx: Int, bottomOfVerticalLinesYPx: Int, timestampTextYPx: Int,
                    jobArea: JobArea, timeZone: ZoneId): immutable.Seq[TagOf[SVGElement]] = {
    val maxHorizontalBar = 5
    (0 to maxHorizontalBar) flatMap { idx =>
      val x = jobArea.widthPx / maxHorizontalBar * idx
      val yStart = topOfVerticalLinesYPx
      val yEnd = bottomOfVerticalLinesYPx
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
          ^.y := timestampTextYPx,
          ^.textAnchor := "middle",
          timeOnBar.format(DateTimeFormatter.ofPattern("uuuu-MMM-dd HH:mm"))
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
          case Right(build) =>
            val startRelativeToDrawingAreaBeginning = (build.buildStart - jobArea.startTime).max(0.seconds)
            val endRelativeToDrawingAreaBeginning = build.maybeBuildFinish match {
              case Some(buildFinish) => (buildFinish - jobArea.startTime).min(jobArea.length)
              case None => jobArea.length
            }

            val buildRectangle = if (endRelativeToDrawingAreaBeginning.toNanos > 0 &&
              startRelativeToDrawingAreaBeginning < jobArea.length) {

              val relativeStartRatio = startRelativeToDrawingAreaBeginning / jobArea.length
              val relativeEndRatio = endRelativeToDrawingAreaBeginning / jobArea.length

              val relativeWidthRatio = relativeEndRatio - relativeStartRatio //todo assert if this is negative, also round up to >10?
              val startPx = relativeStartRatio * jobArea.widthPx
              //todo this will go out of the drawing area, fix
              val widthPx = Math.max(4, relativeWidthRatio * jobArea.widthPx) //todo display these nicely, probably not really a problem
              //todo header, colors, hovering, zooming, horizontal lines, click

              val style: List[TagMod] = List(MyStyles.rectange, build.buildStatus match {
                case Created => MyStyles.created
                case Pending => MyStyles.pending
                case Building => MyStyles.building
                case Failed => MyStyles.failed
                case Successful => MyStyles.success
                case Aborted => MyStyles.aborted
                case Unstable => MyStyles.unstable
              })

              val finishString = build.maybeBuildFinish.map(time => s"Finish: $time\n").getOrElse("")
              val nonStyle = List(
                ^.x := startPx.toInt,
                ^.y := (stripHeight - rectangleHeight) / 2,
                ^.width := widthPx.toInt,
                ^.height := rectangleHeight.toInt,
                className := s"build_rect",
                //todo add length
                //todo replace this with jQuery or sg similar and make it pop up immediately not after delay and not browser dependent way
                <.title(s"Id: ${build.buildNumber.i}\nStart: ${build.buildStart}\n${finishString}Status: ${build.buildStatus}")
              )
              Some(
                a(href := jobState.request.buildUi(build.buildNumber).u.toString(),
                  target := "_blank",
                  <.rect(nonStyle ++ style: _*)
                )
              )
            } else
              None
            buildRectangle.toList
          case Left(error) =>
            logger.warn(s"Build failed to query: ${error.s}")
            None.toList
        })
    }
  }
}
