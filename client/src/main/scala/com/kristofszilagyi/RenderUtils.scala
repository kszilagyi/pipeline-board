package com.kristofszilagyi

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId}

import com.kristofszilagyi.Canvas.className
import com.kristofszilagyi.shared.InstantOps._
import com.kristofszilagyi.shared.JenkinsBuildStatus.Building
import com.kristofszilagyi.shared.TypeSafeEqualsOps._
import com.kristofszilagyi.shared.ZonedDateTimeOps._
import com.kristofszilagyi.shared.{JobDetails, Wart}
import japgolly.scalajs.react.vdom.TagOf
import japgolly.scalajs.react.vdom.svg_<^._
import org.scalajs.dom.raw._
import org.scalajs.dom.svg.SVG

import scala.collection.immutable
import scala.concurrent.duration.{DurationLong, FiniteDuration}

@SuppressWarnings(Array(Wart.Overloading))
object RenderUtils {

  @SuppressWarnings(Array(Wart.DefaultArguments))
  def atPosition(x: Int = 0, y: Int = 0, elements: Seq[TagMod]): TagOf[SVG] = {
    <.svg(elements ++ List(^.x := x, ^.y := y): _*)
  }

  def atPosition(x: Int, y: Int, elements: TagMod): TagOf[SVG] = {
    atPosition(x, y, List(elements))
  }

  def verticalLines(jobAreaWidthPx: Int,
                    backgroundBaseLine: Int => Int, numberOfJobs: Int, jobHeight: Int,
                    endTime: Instant, drawingAreaDuration: FiniteDuration, timeZone: ZoneId): immutable.Seq[TagOf[SVGElement]] = {
    val maxHorizontalBar = 5
    (0 to maxHorizontalBar) flatMap { idx =>
      val x = jobAreaWidthPx / maxHorizontalBar * idx
      val yStart = backgroundBaseLine(0)
      val yEnd = backgroundBaseLine(0) + numberOfJobs * jobHeight + 10
      val timeOnBar = endTime.atZone(timeZone) - drawingAreaDuration + idx.toDouble / maxHorizontalBar * drawingAreaDuration
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

  def jobRectanges(jobState: JobDetails, drawingAreaBeginning: Instant,
                   durationSinceDrawingAreaBeginning: FiniteDuration, jobAreaWidthPx: Int, rectangleHeight: Int, stripHeight: Int): Seq[TagOf[SVGElement]] = {
    jobState.r match {
      case Left(err) =>
        List(<.text(
          ^.fill := "red",
          err.s
        ))
      case Right(runs) =>
        runs.flatMap(either => either match {
          case Right(run) =>
            val startRelativeToDrawingAreaBeginning = (run.buildStart - drawingAreaBeginning).max(0.seconds)
            val endRelativeToDrawingAreaBeginning = (run.buildFinish - drawingAreaBeginning).min(durationSinceDrawingAreaBeginning)
            //todo deal with more than a day longer
            //todo deal with partially inside
            val buildRectangle = if (endRelativeToDrawingAreaBeginning.toNanos > 0 &&
              startRelativeToDrawingAreaBeginning < durationSinceDrawingAreaBeginning) {

              val relativeStartRatio = startRelativeToDrawingAreaBeginning / durationSinceDrawingAreaBeginning
              val relativeEndRatio = if (run.jenkinsBuildStatus ==== Building) {
                1.0
              } else {
                endRelativeToDrawingAreaBeginning / durationSinceDrawingAreaBeginning
              }
              val relativeWidthRatio = relativeEndRatio - relativeStartRatio //todo assert if this is negative, also round up to >10?
              val startPx = relativeStartRatio * jobAreaWidthPx
              //todo this will go out of the drawing area, fix
              val widthPx = Math.max(4, relativeWidthRatio * jobAreaWidthPx) //todo display these nicely, probably not really a problem
              //todo header, colors, hovering, zooming, horizontal lines, click

              Some(<.rect(
                ^.x := startPx.toInt,
                ^.y := (stripHeight - rectangleHeight)/2,
                ^.width := widthPx.toInt,
                ^.height := rectangleHeight.toInt,
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
  }
}
