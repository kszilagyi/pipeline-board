package com.kristofszilagyi.pipelineboard.actors

import java.time.Instant

import akka.typed.scaladsl.Actor
import akka.typed.{ActorRef, Behavior}
import com.kristofszilagyi.pipelineboard.actors.ResultCache.writeToDb
import com.kristofszilagyi.pipelineboard.db.BuildsDb.{BuildRow, buildsQuery}
import com.kristofszilagyi.pipelineboard.fetchers.Fetcher
import com.kristofszilagyi.pipelineboard.shared.TypeSafeEqualsOps._
import com.kristofszilagyi.pipelineboard.shared.Wart.discard
import com.kristofszilagyi.pipelineboard.shared._
import slick.jdbc.SQLiteProfile
import slogging.LazyLogging

import scala.collection.immutable.ListMap
import scala.concurrent.duration.FiniteDuration

sealed trait ResultCacheIncoming
final case class FetchCached(parent: ActorRef[AllGroups]) extends ResultCacheIncoming

final case class FetchedJobBuilds(r: Either[ResponseError, Map[BuildNumber, scala.Either[ResponseError, BuildInfo]]], resultTime: Instant)
final case class OneFetchFinished(desc: Job, builds: FetchedJobBuilds) extends ResultCacheIncoming


final case class CachedJobDetails(desc: Job, maybeError: Option[ResponseError],
                                  builds: Map[BuildNumber, scala.Either[ResponseError, BuildInfo]], latestUpdate: Instant) {
  def merge(newResults: FetchedJobBuilds): CachedJobDetails = {
    newResults.r match {
      case Left(newError) => copy(maybeError = Some(newError))
      case Right(newBuilds) =>
        val mergedBuilds = builds.map { case (buildNumber, maybeOldBuild) =>
          val newValue = newBuilds.get(buildNumber).map { maybeNewBuild =>
            (maybeOldBuild, maybeNewBuild) match {
              case (Right(oldBuild), Left(_)) => Right(oldBuild)
              case (_, newEvent) => newEvent
            }
          }
          buildNumber -> newValue.getOrElse(maybeOldBuild)
        }
        val didExistBefore = mergedBuilds.keySet
        val onlyInNew = newResults.r.getOrElse(Map.empty).filter { case (number, build) =>
          !didExistBefore.contains(number)
        }
        CachedJobDetails(desc, maybeError = None, latestUpdate = newResults.resultTime, builds = onlyInNew ++ mergedBuilds)
    }
  }
}
final case class CachedResult(groups: Seq[(GroupName, Seq[CachedJobDetails])])

object ResultCache {
  private def writeToDb(db: SQLiteProfile.backend.DatabaseDef, fetch: OneFetchFinished): Unit = {
    import SQLiteProfile.api._
    val name = fetch.desc.name
    val builds = fetch.builds.r.getOrElse(Map.empty).values.flatMap(_.toOption.toList)
    val dbRows = builds.map(b => BuildRow(name, b.buildNumber, b.buildStatus, b.buildStart, b.maybeBuildFinish))
    val upserts = dbRows.map(buildsQuery.insertOrUpdate(_))
    discard(db.run(DBIO.sequence(upserts))) //fire and forget
  }
}

final class ResultCache(db: SQLiteProfile.backend.DatabaseDef ,jobGroups: ListMap[GroupName, Seq[Job]], fetchFrequency: FiniteDuration, fetchers: Traversable[Fetcher], buildsInDb: Seq[BuildRow]) extends LazyLogging {
  val behaviour: Behavior[ResultCacheIncoming] = {
    Actor.deferred { ctx =>
      fetchers.foreach{ fetcher =>
        val fetcherRef = ctx.spawn(fetcher.behaviour, fetcher.name)
        ctx.watch(fetcherRef)
        val heartbeatRef = ctx.spawn(new Heartbeat(fetcherRef, ctx.self, fetchFrequency).behaviour, fetcher.name + "-heart")
        ctx.watch(heartbeatRef)
      }

      @SuppressWarnings(Array(Wart.Recursion))
      def b(cache: CachedResult): Behavior[ResultCacheIncoming] = {
        Actor.immutable[ResultCacheIncoming] { (_, msg) =>
          msg match {
            case FetchCached(sender) =>
              sender ! AllGroups(
                cache.groups.map { case (name, cachedJobDetails) =>
                  name -> JobGroup(cachedJobDetails.map { details =>
                    val jobBuilds = JobBuilds(details.maybeError match {
                        case Some(error) =>
                          Left(error)
                        case None =>
                          Right(details.builds.toList.sortBy(_._1.i).map(_._2))
                      },
                      details.latestUpdate
                    )
                    JobDetails(details.desc, jobBuilds)
                  })
                }
              )
              Actor.same
            case newResult: OneFetchFinished =>
              writeToDb(db, newResult)
              val newCache = cache.groups.map {case (name, group) =>
                name -> group.map{ jobDetails =>
                  if (jobDetails.desc ==== newResult.desc) {
                    jobDetails.merge(newResult.builds)
                  } else {
                    jobDetails
                  }

                }
              }
              b(CachedResult(newCache))
          }
        }
      }

      val initialCache = jobGroups.map{case (groupName, jobs) =>
        groupName -> jobs.map { job =>
          val buildInfos = buildsInDb.filter(_.name ==== job.name).map{ build =>
            build.buildNumber -> Right(BuildInfo(build.buildStatus, build.buildStart, build.maybeBuildFinish, build.buildNumber))
          }.toMap
          CachedJobDetails(job, maybeError = None, builds = buildInfos, latestUpdate = Instant.MIN)
        }
      }
      b(CachedResult(initialCache.toSeq))
    }
  }
}
