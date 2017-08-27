package com.kristofszilagyi.shared

import enumeratum.{CirceEnum, EnumEntry}
import enumeratum._


sealed abstract class JenkinsBuildStatus(override val entryName: String) extends EnumEntry

object JenkinsBuildStatus extends Enum[JenkinsBuildStatus] with CirceEnum[JenkinsBuildStatus] {
  val values = findValues

  case object Building extends JenkinsBuildStatus("BUILDING")
  case object Failed extends JenkinsBuildStatus("FAILURE")
  case object Successful extends JenkinsBuildStatus("SUCCESS")
  case object Aborted extends JenkinsBuildStatus("ABORTED")
}