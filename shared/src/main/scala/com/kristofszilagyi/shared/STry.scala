package com.kristofszilagyi.shared

import scala.util.control.NonFatal

/**
  * S stands for serializable
  */
object STry {
  def apply[A](f: => A): STry[A] = {
    try {
      Right(f)
    } catch {
      case NonFatal(ex) => Left(SThrowable.from(ex))
    }
  }
  type STry[+A] = Either[SThrowable, A]
}
