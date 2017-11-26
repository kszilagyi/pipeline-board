package com.kristofszilagyi.shared


import com.kristofszilagyi.shared.pixel.Pixel.RichInt

import scala.concurrent.duration.DurationDouble
import scala.language.postfixOps

object CssSettings {
  val settings = scalacss.DevDefaults
}

import CssSettings.settings._

object MyStyles extends StyleSheet.Inline {

  import dsl._

  val blinker = keyframes(
    (0 %%) -> keyframe(visibility.hidden),
    (50 %%) -> keyframe(visibility.hidden),
    (100 %%) -> keyframe(visibility.visible),
  )

  val rectange = style{
    &.hover(
      opacity(0.7)
    )
  }

  val created = style(svgFill := yellow)

  val pending = style(svgFill := orange)

  val building = style(
    svgFill := purple,
    animationName(blinker),
    animationDuration(2.seconds),
    animationIterationCount.infinite,
    animationTimingFunction.linear
  )

  val failed = style(
    svgFill := red
  )

  val success = style(svgFill := green)

  val aborted = style(svgFill := grey)

  val unstable = style(svgFill := pink)

  val labelEnd = 200.xpx
  val rightMargin = 100.xpx
}
