package com.kristofszilagyi.shared

import java.time.Instant

import io.circe.generic.JsonCodec
import io.circe.java8.time._

@JsonCodec final case class BuildNumber(i: Int)

@JsonCodec final case class JenkinsBuildInfo(jenkinsBuildStatus: JenkinsBuildStatus,
                                  buildStart: Instant,
                                  buildFinish: Instant,
                                  buildNumber: BuildNumber)
