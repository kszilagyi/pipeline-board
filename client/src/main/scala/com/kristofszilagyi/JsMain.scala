package com.kristofszilagyi

import com.kristofszilagyi.Canvas._
import org.scalajs.dom
import slogging.{LoggerConfig, PrintLoggerFactory}

import scala.scalajs.js


object JsMain extends js.JSApp {
  LoggerConfig.factory = PrintLoggerFactory()

  def main(): Unit = {

    println("Application starting")

    val _ = JobCanvas().renderIntoDOM(dom.document.getElementById("root"))
  }
}