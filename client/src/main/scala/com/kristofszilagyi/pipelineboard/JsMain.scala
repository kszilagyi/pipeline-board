package com.kristofszilagyi.pipelineboard

import com.kristofszilagyi.pipelineboard.Canvas._
import org.scalajs.dom
import slogging.{LazyLogging, LoggerConfig, PrintLoggerFactory}


object JsMain extends LazyLogging {
  LoggerConfig.factory = PrintLoggerFactory()

  def main(args: Array[String]): Unit = {

    logger.info("Application starting")
    val _ = JobCanvas().renderIntoDOM(dom.document.getElementById("root"))
  }
}