package controllers
import javax.inject.Inject

import play.api.libs.ws._

import scala.concurrent.{ExecutionContext, Future}

sealed trait BuildStatus
case object Building extends BuildStatus
case object Failed extends BuildStatus
case object Success extends BuildStatus
case object Aborted extends BuildStatus

final case class BuildData(result: BuildStatus)

final case class JobData(allBuilds: Seq[BuildData])

final case class Url(s: String)

final case class JobNumber(i: Int)

object JenkinsFetcher {

}

class JenkinsFetcher @Inject() (ws: WSClient)(implicit ec: ExecutionContext) {
  def query(url: Url): Future[JobData] = {
    val jobNumbers = ws.url(url.s).get.map(response => (response.json \ "builds" \\ "number").map(_.validate[Int].map(JobNumber)))
    jobNumbers.foreach(println)
    ???
  }
}


