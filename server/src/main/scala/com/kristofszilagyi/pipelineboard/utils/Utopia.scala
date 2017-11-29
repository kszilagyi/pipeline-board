package com.kristofszilagyi.pipelineboard.utils

import com.kristofszilagyi.pipelineboard.shared.Wart
import com.kristofszilagyi.pipelineboard.utils.AssertionEx.failEx
import slogging.LazyLogging

import scala.collection.TraversableLike
import scala.collection.generic.CanBuildFrom
import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds
import scala.util.{Failure, Success, Try}


object Utopia extends LazyLogging {
  private final class BugInUtopia(cause: Throwable) extends Throwable(cause)
  def sequence[A, M[X] <: TraversableLike[X, M[X]]](in: M[Utopia[A]])(implicit cbf: CanBuildFrom[M[Utopia[A]], Future[A], M[Future[A]]],
                                                                      cbf2: CanBuildFrom[M[Future[A]], A, M[A]],
                                                                      executor: ExecutionContext): Utopia[M[A]] = {
    val x = in.map(_.unfailableFuture)
    new Utopia[M[A]](Future.sequence[A, M](x))
  }

  def finished[T](t: T): Utopia[T] = new Utopia(Future.successful(t))

  private def logBug(t: Throwable): Nothing = {
    val infodException = new BugInUtopia(t)
    val msg = "Bug in Utopia, please report"
    logger.error(msg, infodException)
    failEx(msg, infodException)
  }

  //this is necessary because the implicit =:= is on the second parameter list therefore the compiler
  //can't infer the type of f (that would be the alternative implementation)
  implicit class RichTryUtopia[T](utopia: Utopia[Try[T]]) {
    @SuppressWarnings(Array(Wart.Overloading))
    def map[S](f: Try[T] => S)(implicit executor: ExecutionContext): Utopia[Try[S]] = {
      new Utopia(utopia.unfailableFuture.map{ tryT =>
        Try(f(tryT))
      })
    }
  }


  implicit class RichUtopia[T](utopia: Utopia[T]) {
    def map[S](f: T => S)(implicit executor: ExecutionContext): Utopia[Try[S]] = {
      utopia.unfailableFuture.map(f).lift
    }

    /**
      * Only use if you know what you are doing.
      * @param f function which never throws
      */
    def noThrowingMap[S](f: T => S)(implicit executor: ExecutionContext): Utopia[S] = {
      new Utopia(utopia.unfailableFuture.map(f))
    }
  }


  implicit class RichFuture[A](future: Future[A]) {
    def lift(implicit ec: ExecutionContext): Utopia[Try[A]] = new Utopia[Try[A]](
      future transform { t =>
        Success(t)
      }
    )
  }

}

final class Utopia[+T] private(private val unfailableFuture: Future[T]) extends LazyLogging {
  import Utopia._

  def onComplete[U](f: T => U)(implicit executor: ExecutionContext): Unit = {
    unfailableFuture.onComplete {
      case Success(value) =>
        f(value)
      case Failure(exception) => //this just can't happen. A lifted future doesn't have an error case
        logBug(exception)
    }
  }

  def value: Option[T] = unfailableFuture.value map {
    case Failure(exception) => //this just can't happen. A lifted future doesn't have an error case
      logBug(exception)
    case Success(t) => t
  }


}
