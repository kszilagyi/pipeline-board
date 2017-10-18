package com.kristofszilagyi

import java.time.Instant

import com.kristofszilagyi.shared.BuildStatus.Successful
import com.kristofszilagyi.shared.SameThreadExecutionContext._
import com.kristofszilagyi.shared._
import com.netaporter.uri.Uri
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
  def dataFeed(): Future[ResultAndTime] = {
    Future.successful(
      ResultAndTime(
        CachedResult(
          Some(
            AllResult(
              List(
                JobDetails(
                  Job(JobName("something"), JobUrl(Uri.parse("example.com")), JobType.Jenkins),
                  Right(Seq(Right(BuildInfo(Successful, Instant.now(), Instant.now(), BuildNumber(1)))))
                )
              ),
              Instant.now()
            )
          )
        ),
        Instant.now()
      )
    )
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
