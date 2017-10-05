package com.kristofszilagyi

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId}

import com.kristofszilagyi.shared.Wart
import com.kristofszilagyi.shared.ZonedDateTimeOps._
import japgolly.scalajs.react.vdom.TagOf
import japgolly.scalajs.react.vdom.svg_<^._
import org.scalajs.dom.raw._
import org.scalajs.dom.svg.SVG

import scala.collection.immutable
import scala.concurrent.duration.FiniteDuration

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

  def strip(jobAreaWidthPx: Int, stripHeight: Int, color: String, elementsInside: Seq[TagMod]) = {

    val background = <.rect(
      ^.height := stripHeight,
      ^.width := jobAreaWidthPx,
      ^.fill := color,
    )
    <.svg(
      elementsInside ++ List(
        ^.height := stripHeight,
        ^.width := jobAreaWidthPx,
        background
      ): _*
    )
  }
}
