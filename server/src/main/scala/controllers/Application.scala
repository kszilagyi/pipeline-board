package controllers

import javax.inject._
import play.api.Configuration
import play.api.mvc._

class Application @Inject() (fetcher: JenkinsFetcher)(implicit val config: Configuration) extends InjectedController {

  def index = Action {
    fetcher.query(Url("http://localhost:8080/job/Other%20stuff/api/json?pretty=true"))
    Ok(views.html.index("MultiCI Dashboard"))
  }

}
