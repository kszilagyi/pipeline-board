package com.kristofszilagyi.shared

import enumeratum.{CirceEnum, EnumEntry}
import enumeratum._


sealed abstract class BuildStatus extends EnumEntry

object BuildStatus extends Enum[BuildStatus] with CirceEnum[BuildStatus] {
  val values = findValues

  case object Building extends BuildStatus
  case object Failed extends BuildStatus
  case object Successful extends BuildStatus
  case object Aborted extends BuildStatus
}