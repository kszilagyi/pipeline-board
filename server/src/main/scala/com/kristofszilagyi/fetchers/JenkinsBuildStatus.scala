package com.kristofszilagyi.fetchers

import com.kristofszilagyi.shared.BuildStatus
import com.kristofszilagyi.utils.TolerantCirceEnum
import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable

sealed abstract class JenkinsBuildStatus extends EnumEntry {
  def toBuildStatus: BuildStatus
}

object JenkinsBuildStatus extends Enum[JenkinsBuildStatus] with TolerantCirceEnum[JenkinsBuildStatus] {
  val values: immutable.IndexedSeq[JenkinsBuildStatus] = findValues

  case object Building extends JenkinsBuildStatus {
    def toBuildStatus: BuildStatus = BuildStatus.Building
  }
  case object Failure extends JenkinsBuildStatus {
    def toBuildStatus: BuildStatus = BuildStatus.Failed
  }
  case object Success extends JenkinsBuildStatus {
    def toBuildStatus: BuildStatus = BuildStatus.Successful
  }
  case object Aborted extends JenkinsBuildStatus {
    def toBuildStatus: BuildStatus = BuildStatus.Aborted
  }
  case object Unstable extends JenkinsBuildStatus {
    def toBuildStatus: BuildStatus = BuildStatus.Unstable
  }
}