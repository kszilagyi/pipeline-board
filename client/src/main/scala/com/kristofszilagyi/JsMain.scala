package com.kristofszilagyi

import com.kristofszilagyi.Canvas._
import org.scalajs.dom
import slogging.{LazyLogging, LoggerConfig, PrintLoggerFactory}

import scala.scalajs.js


object JsMain extends js.JSApp with LazyLogging {
  LoggerConfig.factory = PrintLoggerFactory()

  def main(): Unit = {

    logger.info("Application starting")

    val _ = JobCanvas().renderIntoDOM(dom.document.getElementById("root"))
  }
}