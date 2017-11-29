package com.kristofszilagyi.pipelineboard

import scala.concurrent.duration.FiniteDuration
import scala.scalajs.js
import scala.scalajs.js.timers.SetIntervalHandle


trait JsTimers {
  def setInterval(interval: FiniteDuration, body: => Unit): SetIntervalHandle
  def clearInterval(handle: SetIntervalHandle): Unit
}

object RealJsTimers extends JsTimers {
  def setInterval(interval: FiniteDuration, body:  => Unit): SetIntervalHandle = {
    js.timers.setInterval(interval)(body)
  }

  def clearInterval(handle: SetIntervalHandle): Unit = {
    js.timers.clearInterval(handle)
  }
}