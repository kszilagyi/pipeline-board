package com.kristofszilagyi.fetchers

import com.kristofszilagyi.shared.BuildStatus
import com.kristofszilagyi.utils.TolerantCirceEnum
import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable


sealed trait GitLabCiBuildStatus extends EnumEntry {
}
sealed trait DisplayableGitLabCiStatus extends GitLabCiBuildStatus {
  def toBuildStatus: BuildStatus
}
//created, pending, running, failed, success, canceled, skipped, manual
object GitLabCiBuildStatus extends Enum[GitLabCiBuildStatus] with TolerantCirceEnum[GitLabCiBuildStatus] {
  val values: immutable.IndexedSeq[GitLabCiBuildStatus] = findValues

  case object Created extends DisplayableGitLabCiStatus {
    def toBuildStatus: BuildStatus = BuildStatus.Created
  }
  case object Pending extends DisplayableGitLabCiStatus {
    def toBuildStatus: BuildStatus = BuildStatus.Pending
  }
  case object Running extends DisplayableGitLabCiStatus {
    def toBuildStatus: BuildStatus = BuildStatus.Building
  }
  case object Failed extends DisplayableGitLabCiStatus {
    def toBuildStatus: BuildStatus = BuildStatus.Failed
  }
  case object Success extends DisplayableGitLabCiStatus {
    def toBuildStatus: BuildStatus = BuildStatus.Successful
  }
  case object Canceled extends DisplayableGitLabCiStatus {
    def toBuildStatus: BuildStatus = BuildStatus.Aborted
  }
  case object Skipped extends GitLabCiBuildStatus
  case object Manual extends GitLabCiBuildStatus
}
