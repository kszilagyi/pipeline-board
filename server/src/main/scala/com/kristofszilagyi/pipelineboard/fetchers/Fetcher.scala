package com.kristofszilagyi.pipelineboard.fetchers

import java.net.URLEncoder

import akka.typed.Behavior
import com.kristofszilagyi.pipelineboard.fetchers.JenkinsFetcher.Fetch

object Fetcher {
  def encodeForActorName(s: String): String = URLEncoder.encode(s.replaceAll(" ", "_"), "utf-8")
}
trait Fetcher {
  def name: String
  def behaviour: Behavior[Fetch]
}