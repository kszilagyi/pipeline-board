package com.kristofszilagyi.pipelineboard.fetchers

import java.time.Instant

import akka.typed.scaladsl.Actor
import com.kristofszilagyi.pipelineboard.controllers.{GitLabCiAccessToken, JobNameOnGitLab}
import com.kristofszilagyi.pipelineboard.fetchers.GitLabCiFetcher.GitLabCiJson.PartialJobsInfo
import com.kristofszilagyi.pipelineboard.shared._
import com.netaporter.uri.Uri
import io.circe.generic.JsonCodec
import play.api.libs.ws.{WSClient, WSRequest}
import slogging.{LazyLogging, Logger}
import GitLabCiFetcher._
import TypeSafeEqualsOps.AnyOps
import cats.implicits._
import com.kristofszilagyi.pipelineboard.FetcherResult
import com.kristofszilagyi.pipelineboard.actors.FetchedJobBuilds
import com.kristofszilagyi.pipelineboard.fetchers.JenkinsFetcher.Fetch
import com.kristofszilagyi.pipelineboard.utils.Utopia.RichFuture
import com.kristofszilagyi.pipelineboard.utils.UriOps.RichUriObj

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}


final case class GitLabCiJob(common: Job, maybeAccessToken: Option[GitLabCiAccessToken], jobNameOnGitLab: JobNameOnGitLab) {
  def firstPage: RawUrl = common.jobInfo


  def authenticatedRestRequest(uri: RawUrl, ws: WSClient): WSRequest = {
    val headers = maybeAccessToken.map { accessToken =>
      "PRIVATE-TOKEN" -> accessToken.s
    }.toList
    ws.url(uri.rawString).withHttpHeaders(headers: _*)
  }
}

object GitLabCiFetcher {

  private val loggerName = "GitLabCiFetcher" //slogger has a macro which needs this...
  @SuppressWarnings(Array(Wart.Public))
  object GitLabCiJson {
    import io.circe.java8.time._

    @JsonCodec
    final case class PartialJobInfo(id: Int, name: String, created_at: Instant,
                                    started_at: Option[Instant], finished_at: Option[Instant],
                                    status: GitLabCiBuildStatus) {

      def buildNumber: BuildNumber = BuildNumber(id)
    }
    type PartialJobsInfo = Seq[PartialJobInfo]
  }

  private def extractNextPageLinkFromLinkHeader(requestUri: RawUrl, linkString: String) = {

    final case class RawLink(rel: String, link: String)
    final case class Link(rel: String, link: RawUrl)
    val linkStrings = linkString.split(",").map(_.split(";").map(_.trim))
    val links = linkStrings.map { link =>
      val relPrefix = "rel="
      def cleanRel(s: String) = s.drop(relPrefix.length).trim.replaceAll("\"", "")

      link.toList match {
        case List(one, other) =>
          if (one.startsWith(relPrefix)) {
            Right(RawLink(rel = cleanRel(one), link = other))
          } else if (other.startsWith(relPrefix)) {
            Right(RawLink(rel = cleanRel(other), link = one))
          } else {
            Left(ResponseError(s"Link header doesn't contain properly formed rel part. Parts are: $one, $other"))
          }

        case malformed =>
          Left(ResponseError(s"Link header doesn't contain 2 fields: $malformed. (${requestUri.rawString})"))
      }
    }
    val maybeSequencedLinks: Either[ResponseError, List[RawLink]] = links.toList.sequenceU //type annotation to make intellj faster
    val checkedLinks: Either[ResponseError, List[Link]] = maybeSequencedLinks.map { ls =>
      val maybeLinks: Either[ResponseError, List[Link]] = ls.map { link =>
        Uri.safeParse(link.link.drop(1).dropRight(1)) match { //first and last character is < and >
          case Left(notAUri) => Left(ResponseError(notAUri.s))
          case Right(uri) => Right(Link(link.rel, RawUrl(uri)))
        }
      }.sequenceU
      maybeLinks
    }.flatten
    val maybeMaybeNext: Either[ResponseError, Option[RawUrl]] = checkedLinks.map(links =>
      links.find(_.rel ==== "next").map(link =>
        link.link
      )
    )
    maybeMaybeNext
  }

  private def extractNextPageLink(requestUrl: RawUrl, result: WSRequest#Response): Either[ResponseError, Option[RawUrl]] = {
    if (result.status !=== 200) {
      Left(ResponseError(s"Invalid status code while paging (${result.status})"))
    } else {
      val maybeLinkHeaderString = result.header("Link") match {
        case Some(linkHeader) => Right(linkHeader)
        case None => Left(ResponseError("Link header is missing"))
      }

      maybeLinkHeaderString flatMap { linkString: String =>
        extractNextPageLinkFromLinkHeader(requestUrl, linkString)
      }
    }
  }

  @SuppressWarnings(Array(Wart.Recursion))
  private def queryOnePage(logger: Logger, job: GitLabCiJob, url: RawUrl, ws: WSClient, pagesToQuery: Int)
                          (implicit ec: ExecutionContext): Future[Either[ResponseError, PartialJobsInfo]] = {
    if (pagesToQuery > 0) {
      logger.info(s"Querying $url")
      val request = job.authenticatedRestRequest(url, ws)
      request.get.map { result =>
        val maybeMaybeNext = extractNextPageLink(url, result)
        val maybeCurrentResults = safeRead[PartialJobsInfo](url, result)
        logger.info(s"Number of current results: ${maybeCurrentResults.map(_.size).getOrElse(0)}")
        val normalizedRecursion: Future[Either[ResponseError, GitLabCiFetcher.GitLabCiJson.PartialJobsInfo]] = maybeMaybeNext.map {
          case Some(next) =>
            queryOnePage(logger, job, next, ws, pagesToQuery = pagesToQuery - 1).map { maybeNextResults =>
              val allresults = maybeCurrentResults.flatMap{ currentResults =>
                maybeNextResults.map { nextResults =>
                  currentResults ++ nextResults
                }
              }
              allresults
            }
          case None => Future.successful(maybeCurrentResults)
        } match {
          case Left(err) => Future.successful(Left(err))
          case Right(queryNextFut) => queryNextFut
        }
        normalizedRecursion
      }.flatten
    } else {
      Future.successful(Right(Seq.empty))
    }
  }

  def queryLastNBuildPages(logger: Logger, job: GitLabCiJob, ws: WSClient, pagesToQuery: Int)
                          (implicit ec: ExecutionContext): Future[Either[ResponseError, PartialJobsInfo]] = {
    queryOnePage(logger, job, job.firstPage, ws, pagesToQuery = pagesToQuery)
  }

}
//todo probably this should be a future....
final class GitLabCiFetcher(ws: WSClient, jobToFetch: GitLabCiJob,
                            buildPagesToQuery: Int)(implicit ec: ExecutionContext) extends LazyLogging with Fetcher {
  def behaviour: Actor.Immutable[Fetch] = Actor.immutable[Fetch] { (_, msg) =>
    msg match {
      case Fetch(replyTo) =>
        val resultFut = {
          val lastNBuildPagesForProjectFut = queryLastNBuildPages(logger, jobToFetch, ws, buildPagesToQuery)
          val buildsWithRightNameFut = lastNBuildPagesForProjectFut.map{ last1000BuildsForProject =>
            last1000BuildsForProject.map(_.filter(_.name ==== jobToFetch.jobNameOnGitLab.s))
          }
          val buildsFut = buildsWithRightNameFut.map{ buildsWithRightName =>
            buildsWithRightName.map(_.flatMap { build =>
              build.status match {
                case status: DisplayableGitLabCiStatus =>
                  build.started_at.map(start =>  build.buildNumber -> Right(BuildInfo(status.toBuildStatus, buildStart = start,
                    maybeBuildFinish = build.finished_at, build.buildNumber))).toList
                case _ => None.toList
              }
            }.toMap)
          }
          buildsFut.lift noThrowingMap {
            case Failure(ex) => FetcherResult(jobToFetch.common,
              FetchedJobBuilds(Left(ResponseError.failedToConnect(jobToFetch.firstPage, ex)), Instant.now()))
            case Success(builds) => FetcherResult(jobToFetch.common, FetchedJobBuilds(builds, Instant.now()))
          }
        }
        resultFut.onComplete { result =>
          replyTo ! result
        }
        Actor.same
    }
  }

  def name: String = {
    val encodedName = Fetcher.encodeForActorName(jobToFetch.common.name.s)
    s"gitLabCi-$encodedName"
  }
}
