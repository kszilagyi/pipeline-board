package com.kristofszilagyi.pipelineboard.fetchers

import com.kristofszilagyi.pipelineboard.shared.BuildStatus
import com.kristofszilagyi.pipelineboard.utils.TolerantCirceEnum
import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable

sealed trait TeamCityBuildStatus extends EnumEntry {
  def toBuildStatus: BuildStatus
}

object TeamCityBuildStatus extends Enum[TeamCityBuildStatus] with TolerantCirceEnum[TeamCityBuildStatus] {
  def values: immutable.IndexedSeq[TeamCityBuildStatus] = findValues

  case object Success extends TeamCityBuildStatus {
    def toBuildStatus: BuildStatus = BuildStatus.Successful
  }
  case object Failure extends TeamCityBuildStatus {
    def toBuildStatus: BuildStatus = BuildStatus.Failed
  }
  case object Error extends TeamCityBuildStatus {
    def toBuildStatus: BuildStatus = BuildStatus.Failed
  }

  case object Unknown extends TeamCityBuildStatus {
    def toBuildStatus: BuildStatus = BuildStatus.Aborted
  }
}


sealed trait TeamCityBuildState extends EnumEntry {
  def toBuildStatus(buildStatus: TeamCityBuildStatus): BuildStatus
}


object TeamCityBuildState extends Enum[TeamCityBuildState] with TolerantCirceEnum[TeamCityBuildState] {
  def values: immutable.IndexedSeq[TeamCityBuildState] = findValues

  case object Queued extends TeamCityBuildState {
    def toBuildStatus(buildStatus: TeamCityBuildStatus): BuildStatus = BuildStatus.Pending
  }
  case object Running extends TeamCityBuildState {
    def toBuildStatus(buildStatus: TeamCityBuildStatus): BuildStatus = BuildStatus.Building
  }

  case object Finished extends TeamCityBuildState {
    def toBuildStatus(buildStatus: TeamCityBuildStatus): BuildStatus = {
      buildStatus.toBuildStatus
    }
  }
}