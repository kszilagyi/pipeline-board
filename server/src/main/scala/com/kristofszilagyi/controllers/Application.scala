package com.kristofszilagyi.controllers

import javax.inject._

import com.kristofszilagyi.fetchers.{JenkinsFetcher, JenkinsJobUrl}
import com.netaporter.uri.Uri
import play.api.Configuration
import play.api.mvc._

class Application @Inject() (fetcher: JenkinsFetcher)(val config: Configuration) extends InjectedController {

  def index: Action[AnyContent] = Action {
    fetcher.query(JenkinsJobUrl(Uri.parse("http://localhost:8080/job/Other%20stuff")))
    Ok(views.html.index("Multi CI Dashboard")(config))
  }

}
