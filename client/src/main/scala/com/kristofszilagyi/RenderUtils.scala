package com.kristofszilagyi

import java.time.{Instant, ZoneId}
import java.time.format.DateTimeFormatter

import com.kristofszilagyi.shared.ZonedDateTimeOps._
import japgolly.scalajs.react.vdom.TagOf
import japgolly.scalajs.react.vdom.svg_<^._
import org.scalajs.dom.raw._

import scala.collection.immutable
import scala.concurrent.duration.FiniteDuration
object RenderUtils {

  def verticalLines(labelEndPx: Int, jobAreaWidthPx: Int,
                    backgroundBaseLine: Int => Int, numberOfJobs: Int, jobHeight: Int,
                    endTime: Instant, drawingAreaDuration: FiniteDuration, timeZone: ZoneId): immutable.Seq[TagOf[SVGElement]] = {
    val maxHorizontalBar = 5
    (0 to maxHorizontalBar) flatMap { idx =>
      val x = jobAreaWidthPx / maxHorizontalBar * idx + labelEndPx
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
}
