package com.kristofszilagyi.utils

import slogging.{LogLevel, LoggerConfig, SLF4JLoggerFactory}
import utest.TestSuite

trait TestSuiteWithLogging extends TestSuite {
  LoggerConfig.factory = SLF4JLoggerFactory()
  LoggerConfig.level = LogLevel.DEBUG
}
