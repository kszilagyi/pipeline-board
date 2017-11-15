package com.kristofszilagyi.fetchers

import java.net.URLEncoder

import akka.typed.Behavior
import com.kristofszilagyi.fetchers.JenkinsFetcher.Fetch

object Fetcher {
  def encodeForActorName(s: String): String = URLEncoder.encode(s.replaceAll(" ", "_"), "utf-8")
}
trait Fetcher {
  def name: String
  def behaviour: Behavior[Fetch]
}