package com.kristofszilagyi.shared

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

  //^.style := (animation := "blinker 2s linear infinite")*/


  val building = style(
    svgFill := grey,
    animationName(blinker),
    animationDuration(2.seconds),
    animationIterationCount.infinite,
    animationTimingFunction.linear
  )

  val failed = style(
    svgFill := red
  )

  val success = style(svgFill := green)

  val aborted = style(svgFill := purple)
}
