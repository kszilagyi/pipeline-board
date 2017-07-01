package com.kristofszilagyi.utils

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}


object FutureUtils {

  implicit class RichFuture[A](f: Future[A]) {
    //todo unit test
    def lift(implicit ec: ExecutionContext): Future[Try[A]] = f transform { t =>
      t match {
        case f: Failure[A] => Success(f)
        case s: Success[A] => Success(s)
      }
    }
  }
}
