package com.kristofszilagyi.shared

object TypeSafeEqualsOps {
  @SuppressWarnings(Array(Wart.Equals))
  implicit final class AnyOps[A](self: A) {
    def ====(other: A): Boolean = self == other
    def !===(other: A): Boolean = self != other
  }
}
