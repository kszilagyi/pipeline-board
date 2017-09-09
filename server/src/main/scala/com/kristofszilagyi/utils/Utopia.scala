package com.kristofszilagyi.utils

import com.kristofszilagyi.utils.AssertionEx.fail
import slogging.LazyLogging
import scala.language.higherKinds
import scala.collection.TraversableLike
import scala.collection.generic.CanBuildFrom
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object Utopia {
  def sequence[A, M[X] <: TraversableLike[X, M[X]]](in: M[Utopia[A]])(implicit cbf: CanBuildFrom[M[Utopia[A]], Future[A], M[Future[A]]],
                                                                      cbf2: CanBuildFrom[M[Future[A]], A, M[A]],
                                                                      executor: ExecutionContext): Utopia[M[A]] = {
    val x = in.map(_.future)
    new Utopia[M[A]](Future.sequence[A, M](x))
  }

  def successful[T](t: T): Utopia[T] = new Utopia(Future.successful(t))
}

final class Utopia[T](private val future: Future[T]) extends LazyLogging{
  def onComplete[U](f: T => U)(implicit executor: ExecutionContext): Unit = {
    future.onComplete {
      case Success(value) =>
        f(value)
      case Failure(exception) => //this just can't happen. A lifted future doesn't have an error case
        logger.error("Can't happen", exception)
        fail("Can't happen")
    }
  }

  def map[S](f: T => S)(implicit executor: ExecutionContext): Utopia[S] = {
    new Utopia(future.map(f))
  }

  def value: Option[T] = future.value map {
    case Failure(exception) =>
      logger.error("Can't happen", exception)
      fail("Can't happen")
    case Success(t) => t
  }


}
