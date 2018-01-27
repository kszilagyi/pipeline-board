package com.kristofszilagyi.pipelineboard

import java.time.Instant

import slick.jdbc.SQLiteProfile.api._
import slick.ast.BaseTypedType
import slick.jdbc.{JdbcType, SQLiteProfile}

package object shared {
  implicit val jobDisplaynameType: JdbcType[JobDisplayName] with BaseTypedType[JobDisplayName] =
    SQLiteProfile.MappedColumnType.base[JobDisplayName, String](_.s, JobDisplayName.apply)

  implicit val buildNumberType: JdbcType[BuildNumber] with BaseTypedType[BuildNumber] =
    SQLiteProfile.MappedColumnType.base[BuildNumber, Int](_.i, BuildNumber.apply)

  implicit val buildStatusType: JdbcType[BuildStatus] with BaseTypedType[BuildStatus] =
    SQLiteProfile.MappedColumnType.base[BuildStatus, String](_.entryName, BuildStatus.withNameInsensitive)

  implicit val instantType: JdbcType[Instant] with BaseTypedType[Instant] =
    SQLiteProfile.MappedColumnType.base[Instant, Long](_.toEpochMilli, Instant.ofEpochMilli)

}
