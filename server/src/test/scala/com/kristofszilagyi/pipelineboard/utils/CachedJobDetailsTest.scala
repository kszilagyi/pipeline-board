package com.kristofszilagyi.pipelineboard.utils

import java.time.Instant

import com.kristofszilagyi.pipelineboard.actors.{CachedJobDetails, FetchedJobBuilds}
import com.kristofszilagyi.pipelineboard.shared.JobType.Jenkins
import com.kristofszilagyi.pipelineboard.shared._
import com.netaporter.uri.Uri
import utest._
import utest.framework.{Test, Tree}



object CachedJobDetailsTest extends TestSuite {

  val job = Job(JobDisplayName("job"), Urls(UserRoot(RawUrl(Uri("localhost"))),
    RestRoot(RawUrl(Uri("localhost")))), Jenkins)
  def tests: Tree[Test] = this {
    'emptyPlusOne {
      val bn = BuildNumber(1)
      val bi = BuildInfo(BuildStatus.Successful, Instant.ofEpochMilli(1), Some(Instant.ofEpochMilli(2)), bn)
      CachedJobDetails(job, maybeError = None, builds = Map.empty, latestUpdate = Instant.ofEpochMilli(15))
        .merge(FetchedJobBuilds(Right(Map(bn -> Right(bi))), Instant.ofEpochMilli(20))) ==>
        CachedJobDetails(job, maybeError = None, builds = Map(bn -> Right(bi)), latestUpdate = Instant.ofEpochMilli(20))
    }
  }
}
