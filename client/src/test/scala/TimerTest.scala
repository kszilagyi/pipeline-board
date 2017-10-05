package com.kristofszilagyi

import java.time.Instant

import com.kristofszilagyi.shared.JenkinsBuildStatus.Successful
import com.kristofszilagyi.shared.SameThreadExecutionContext._
import com.kristofszilagyi.shared._
import japgolly.scalajs.react.test.ReactTestUtils
import utest._
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.scalajs.js.timers.SetIntervalHandle

final class MockTimers extends JsTimers {
  private val counter = Iterator.from(0)

  @SuppressWarnings(Array(Wart.Var))
  private var actions: Map[Int, () => Unit] = Map.empty

  @SuppressWarnings(Array(Wart.AsInstanceOf))
  def setInterval(interval: FiniteDuration, body: => Unit): SetIntervalHandle = {
    val id = counter.next()
    actions = actions + (id -> (() => body))
    id.asInstanceOf[SetIntervalHandle]
  }

  @SuppressWarnings(Array(Wart.AsInstanceOf))
  def clearInterval(handle: SetIntervalHandle): Unit = {
    val id = handle.asInstanceOf[Int]
    actions = actions - id
  }

  def executeAll(): Unit = {
    actions.values.foreach(body => body())
  }
}

final class MockAutowire extends MockableAutowire {
  def dataFeed(): Future[BulkFetchResult] = {
    Future.successful(BulkFetchResult(List(JobDetails(Url("example.com"),
      Right(Seq(Right(JenkinsBuildInfo(Successful, Instant.now(), Instant.now(), BuildNumber(1)))))
    ))))
  }
}

object TimerTest extends TestSuite{
  val mockTimers = new MockTimers()
  val TestTimer = Canvas.jobCanvas(mockTimers, new MockAutowire())

  def tests = TestSuite {
    'InitialChangeOfValue {
      ReactTestUtils.withRenderedIntoBody(TestTimer()) { testTimer =>
        println(testTimer.getDOMNode.textContent)
        mockTimers.executeAll()
        println(testTimer.getDOMNode.textContent)
      }
    }
  }
}
