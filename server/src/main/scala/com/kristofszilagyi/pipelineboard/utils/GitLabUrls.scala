package com.kristofszilagyi.pipelineboard.utils

import java.net.URLEncoder

import com.kristofszilagyi.pipelineboard.controllers.Application.utf8
import com.kristofszilagyi.pipelineboard.shared.{RawUrl, RestRoot, UserRoot}
import com.netaporter.uri.PathPart

object GitLabUrls {
  def restRoot(userRoot: UserRoot): RestRoot = {
    val jobPath = URLEncoder.encode(userRoot.u.u.pathParts.map(_.part).mkString("/"), utf8)
    RestRoot(RawUrl(userRoot.u.u.copy(pathParts = Seq(PathPart("api"), PathPart("v4"), PathPart("projects")) :+ PathPart(jobPath))))
  }
}
