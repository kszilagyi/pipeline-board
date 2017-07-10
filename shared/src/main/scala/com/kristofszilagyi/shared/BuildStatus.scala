package com.kristofszilagyi.shared

import enumeratum.{CirceEnum, EnumEntry}
import enumeratum._


sealed abstract class BuildStatus(override val entryName: String) extends EnumEntry

object BuildStatus extends Enum[BuildStatus] with CirceEnum[BuildStatus] {
  val values = findValues

  case object Building extends BuildStatus("BUILDING")
  case object Failed extends BuildStatus("FAILED")
  case object Successful extends BuildStatus("SUCCESS")
  case object Aborted extends BuildStatus("ABORTED")
}