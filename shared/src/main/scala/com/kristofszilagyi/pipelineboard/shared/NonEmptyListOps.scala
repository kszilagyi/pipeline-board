package com.kristofszilagyi.pipelineboard.shared

import cats.data.NonEmptyList


object NonEmptyListOps {
  implicit class RichNonEmptyList[+A](xs: NonEmptyList[A]) {

    def choose(idx: Int): A = {
      val list = xs.toList
      list(idx % list.size)
    }
  }
}
