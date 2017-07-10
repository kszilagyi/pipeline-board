package com.kristofszilagyi.shared

import scala.concurrent.Future

trait AutowireApi{
  def dataFeed(): Future[FetchResult]
}