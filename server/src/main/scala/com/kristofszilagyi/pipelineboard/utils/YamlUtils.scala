package com.kristofszilagyi.pipelineboard.utils

import net.jcazevedo.moultingyaml.{YamlFormat, YamlNumber, YamlString, YamlValue, deserializationError}

object YamlUtils {
  def wrappedYamlString[T](fromString: String => T)(convertToString: T => String): YamlFormat[T] = new YamlFormat[T] {
    def read(yaml: YamlValue): T = yaml match {
      case YamlString(s) => fromString(s)
      case other => deserializationError(s"Should be a string got: $other")
    }

    def write(t: T): YamlValue = YamlString(convertToString(t))
  }

  def wrappedYamlNumber[T](fromBigDecimal: BigDecimal => T)(convertToBigDecimal: T => BigDecimal): YamlFormat[T] = new YamlFormat[T] {
    def read(yaml: YamlValue): T = yaml match {
      case YamlNumber(n) => fromBigDecimal(n)
      case other => deserializationError(s"Should be a number got: $other")
    }

    def write(t: T): YamlValue = YamlNumber(convertToBigDecimal(t))
  }

}
