package com.kristofszilagyi.pipelineboard.db

import java.time.Instant

import com.kristofszilagyi.pipelineboard.shared.{ BuildNumber, BuildStatus, JobDisplayName}

import slick.jdbc.SQLiteProfile.api._

object BuildsDb {
  import com.kristofszilagyi.pipelineboard.shared.instantType
  final case class BuildRow(name: JobDisplayName,
                            buildNumber: BuildNumber,
                            buildStatus: BuildStatus,
                            buildStart: Instant,
                            maybeBuildFinish: Option[Instant]) {
  }

  final class BuildsTable(tag: Tag) extends Table[BuildRow](tag, "builds") {
    import slick.lifted.Shape._
    def name = column[JobDisplayName]("name")
    def buildNumber = column[BuildNumber]("buildNumber")
    def buildStatus = column[BuildStatus]("buildStatus")
    def buildStart = column[Instant]("buildStart")
    def maybeBuildFinish = column[Option[Instant]]("maybeBuildFinish")


    def * =
      (name, buildNumber, buildStatus, buildStart, maybeBuildFinish) <> (BuildRow.tupled, BuildRow.unapply)

  }

  val buildsQuery = TableQuery[BuildsTable]
}