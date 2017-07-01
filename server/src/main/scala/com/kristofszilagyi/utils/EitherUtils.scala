package com.kristofszilagyi.utils

import scala.concurrent.Future
import scala.util.Try


object EitherUtils {

  //: Seq[Either[DeserialisationError, Future[DEither.DEither[BuildStatus]]]]
  implicit class RichEitherOfFuture[E, A](either: Either[E, Future[A]]) {
    def flipLift: Future[Either[E, Try[A]]] = ???
  }

  implicit class RichEither[E, A](either: Either[E, A]) {
    //def flip:
  }

}
