package com.kristofszilagyi

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId}

import com.kristofszilagyi.shared.BuildStatus.{Aborted, Building, Created, Failed, Pending, Successful, Unstable}
import com.kristofszilagyi.shared.InstantOps._
import com.kristofszilagyi.shared.ZonedDateTimeOps._
import com.kristofszilagyi.shared.{JobDetails, MyStyles, Wart}
import japgolly.scalajs.react.vdom.PackageBase.VdomAttr
import japgolly.scalajs.react.vdom.svg_<^.{^, _}
import japgolly.scalajs.react.vdom.{SvgTagOf => _, TagMod => _, _}
import org.scalajs.dom.raw._
import org.scalajs.dom.svg.{A, G, SVG}
import slogging.LazyLogging
import TypeSafeAttributes._
import com.kristofszilagyi.shared.pixel.Pixel._

import scala.collection.immutable
import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scalacss.ScalaCssReact._

@SuppressWarnings(Array(Wart.Overloading))
object RenderUtils extends LazyLogging {

  def alignmentBaseline: VdomAttr[Any] = VdomAttr("alignmentBaseline")
  def dominantBaseline: VdomAttr[Any] = VdomAttr("dominantBaseline")

  def className: VdomAttr[Any] = VdomAttr("className")

  def target: VdomAttr[Any] = VdomAttr("target")

  def a: SvgTagOf[A] = SvgTagOf[A]("a")

  def href: VdomAttr[Any] = VdomAttr("href")

  def textDecoration = VdomAttr("textDecoration")

  def animation = VdomAttr("animation")

  val textAnchorEnd = "end"

  val alignmentBaselineMiddle = "middle"
  val dominantBaselineCentral = "central"

  final case class JobArea(width: WPixel, endTime: Instant, drawingAreaDuration: FiniteDuration) {
    def startTime: Instant = endTime - drawingAreaDuration
    def length: FiniteDuration = endTime - startTime
  }
  @SuppressWarnings(Array(Wart.DefaultArguments))
  def nestAt(x: XPixel = 0.xpx, y: YPixel = 0.ypx, elements: Seq[TagMod]): TagOf[SVG] = {
    <.svg(elements: _*).x(x).y(y)
  }

  def nestAt(x: XPixel, y: YPixel, elements: TagMod): TagOf[SVG] = {
    nestAt(x, y, List(elements))
  }

  @SuppressWarnings(Array(Wart.DefaultArguments))
  def moveTo(x: XPixel = 0.xpx, y: YPixel = 0.ypx, elements: Seq[TagMod]): TagOf[G] = {
    <.g(elements ++ List(^.transform := s"translate($x, $y)"): _*)
  }

  def moveTo(x: XPixel, y: YPixel, elements: TagMod): TagOf[G] = {
    moveTo(x, y, List(elements))
  }

  def verticalLines(topOfVerticalLines: YPixel, bottomOfVerticalLines: YPixel, timestampText: YPixel,
                    jobArea: JobArea, timeZone: ZoneId): immutable.Seq[TagOf[SVGElement]] = {
    val maxHorizontalBar = 5
    (0 to maxHorizontalBar) flatMap { idx =>
      val x = (jobArea.width / maxHorizontalBar * idx).toX
      val yStart = topOfVerticalLines
      val yEnd = bottomOfVerticalLines
      val timeOnBar = jobArea.endTime.atZone(timeZone) - jobArea.drawingAreaDuration + idx.toDouble / maxHorizontalBar * jobArea.drawingAreaDuration
      List(
        <.line(
          ^.strokeWidth := "1",
          ^.stroke := "grey"
        ).x1(x)
         .y1(yStart)
         .x2(x)
         .y2(yEnd),
        <.text(
          ^.textAnchor := "middle",
          timeOnBar.format(DateTimeFormatter.ofPattern("uuuu-MMM-dd HH:mm"))
        ).x(x)
         .y(timestampText)
      )
    }
  }

  def strip(jobAreaWidth: WPixel, stripHeight: HPixel, color: String, elementsInside: Seq[TagMod]): TagOf[SVG] = {

    val background = <.rect(
      ^.fill := color,
    ).height(stripHeight)
     .width(jobAreaWidth)
    <.svg(
      List(
        background
      ) ++ elementsInside: _*
    ).height(stripHeight)
     .width(jobAreaWidth)
  }

  def jobRectanges(jobState: JobDetails, jobArea: JobArea, rectangleHeight: HPixel, stripHeight: HPixel): Seq[TagOf[SVGElement]] = {
    jobState.maybeDynamic match {
      case None =>
        List(<.text(
          ^.fill := "red",
          alignmentBaseline := alignmentBaselineMiddle,
          "No data yet"
        ).y(stripHeight.toY/2),
        )
      case Some(dynamic) =>
        dynamic.r match {
          case Left(err) =>
            List(<.text(
              ^.fill := "red",
              alignmentBaseline := alignmentBaselineMiddle,
              err.s
            ).y(stripHeight.toY/2))
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
                  val startPx = (jobArea.width * relativeStartRatio).toX
                  //todo this will go out of the drawing area, fix
                  val width = 4.wpx.max(jobArea.width * relativeWidthRatio) //todo display these nicely, probably not really a problem
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
                    className := s"build_rect",
                    //todo add length
                    //todo replace this with jQuery or sg similar and make it pop up immediately not after delay and not browser dependent way
                    <.title(s"Id: ${build.buildNumber.i}\nStart: ${build.buildStart}\n${finishString}Status: ${build.buildStatus}")
                  )
                  Some(
                    a(href := jobState.static.buildUi(build.buildNumber).u.toString(),
                      target := "_blank",
                      <.rect(nonStyle ++ style: _*)
                        .x(startPx)
                        .y(((stripHeight - rectangleHeight) / 2).toY)
                        .width(width)
                        .height(rectangleHeight)
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
}
