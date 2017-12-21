package com.kristofszilagyi.pipelineboard.utils
import com.kristofszilagyi.pipelineboard.shared.JobType.GitLabCi
import com.kristofszilagyi.pipelineboard.shared.{RawUrl, RestRoot, Urls, UserRoot}
import com.netaporter.uri.Uri
import utest.TestSuite
import utest._
import utest.framework.{Test, Tree}


object GitLabUrlsTest extends TestSuite {
  def tests: Tree[Test] = this{
    'base {
      GitLabUrls.restRoot(UserRoot(RawUrl(Uri.parse("https://gitlab.com/DemoPipelineBoard/DemoPipelineProject")))).u.rawString ==>
        "https://gitlab.com/api/v4/projects/DemoPipelineBoard%2FDemoPipelineProject"
    }

    'firstpage {
      val user = UserRoot(RawUrl(Uri.parse("https://gitlab.com/DemoPipelineBoard/DemoPipelineProject")))
      val rest = GitLabUrls.restRoot(user)
      GitLabCi.jobInfo(Urls(user, rest)).rawString ==>
        "https://gitlab.com/api/v4/projects/DemoPipelineBoard%2FDemoPipelineProject/jobs?per_page=100"
    }

    'urilibs {
      import com.netaporter.uri.dsl._
      Uri.parse("google.com") / "jobs" ? ("per_page" -> 100)
    }
  }
}



