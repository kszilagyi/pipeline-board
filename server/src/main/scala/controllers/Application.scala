package controllers

import javax.inject._
import play.api.Configuration
import play.api.mvc._

class Application @Inject()(implicit val config: Configuration) extends InjectedController {

  def index = Action {
    Ok(views.html.index("MultiCI Dashboard"))
  }

}
