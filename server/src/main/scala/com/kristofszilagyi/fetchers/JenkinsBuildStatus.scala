package com.kristofszilagyi.fetchers

import com.kristofszilagyi.shared.BuildStatus
import enumeratum.{CirceEnum, Enum, EnumEntry}

import scala.collection.immutable

sealed abstract class JenkinsBuildStatus(override val entryName: String) extends EnumEntry {
  def toBuildStatus: BuildStatus
}

object JenkinsBuildStatus extends Enum[JenkinsBuildStatus] with CirceEnum[JenkinsBuildStatus] {
  val values: immutable.IndexedSeq[JenkinsBuildStatus] = findValues

  case object Building extends JenkinsBuildStatus("BUILDING") {
    override def toBuildStatus: BuildStatus = BuildStatus.Building
  }
  case object Failed extends JenkinsBuildStatus("FAILURE") {
    override def toBuildStatus: BuildStatus = BuildStatus.Failed
  }
  case object Successful extends JenkinsBuildStatus("SUCCESS") {
    override def toBuildStatus: BuildStatus = BuildStatus.Successful
  }
  case object Aborted extends JenkinsBuildStatus("ABORTED") {
    override def toBuildStatus: BuildStatus = BuildStatus.Aborted
  }
}