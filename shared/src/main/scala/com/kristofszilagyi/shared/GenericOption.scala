package com.kristofszilagyi.shared

import scala.language.higherKinds

sealed trait GenericOption[A, Self[X] <: GenericOption[X, Self]]{

  def isEmpty: Boolean
//
//  def isDefined: Boolean = !isEmpty
//
  def get: A
//
//  @inline final def getOrElse[B >: A](default: => B): B =
//  if (isEmpty) default else this.get
//  @inline final def orNull[A1 >: A](implicit ev: Null <:< A1): A1 = this getOrElse ev(null)

  def map[B](f: A => B): Self[B]
 // if (isEmpty) GenericNone[Nothing] else GenericSome(f(this.get))

//  @inline final def fold[B](ifEmpty: => B)(f: A => B): B =
//  if (isEmpty) ifEmpty else f(this.get)
//
//
//  @inline final def flatMap[B](f: A => GenericOption[B]): GenericOption[B] =
//  if (isEmpty) None else f(this.get)
//
//  def flatten[B](implicit ev: A <:< GenericOption[B]): GenericOption[B] =
//    if (isEmpty) None else ev(this.get)
//
//  @inline final def filter(p: A => Boolean): GenericOption[A] =
//  if (isEmpty || p(this.get)) this else None
//
//  @inline final def filterNot(p: A => Boolean): GenericOption[A] =
//  if (isEmpty || !p(this.get)) this else None
//
//
//  final def nonEmpty = isDefined
//
//
//  @inline final def withFilter(p: A => Boolean): WithFilter = new WithFilter(p)
//
//
//  class WithFilter(p: A => Boolean) {
//    def map[B](f: A => B): GenericOption[B] = self filter p map f
//    def flatMap[B](f: A => GenericOption[B]): GenericOption[B] = self filter p flatMap f
//    def foreach[U](f: A => U): Unit = self filter p foreach f
//    def withFilter(q: A => Boolean): WithFilter = new WithFilter(x => p(x) && q(x))
//  }
//
//  final def contains[A1 >: A](elem: A1): Boolean =
//    !isEmpty && this.get == elem
//
//  @inline final def exists(p: A => Boolean): Boolean =
//  !isEmpty && p(this.get)
//
//  @inline final def forall(p: A => Boolean): Boolean = isEmpty || p(this.get)
//
//  @inline final def foreach[U](f: A => U) {
//    if (!isEmpty) f(this.get)
//  }
//
//  @inline final def collect[B](pf: PartialFunction[A, B]): GenericOption[B] =
//  if (!isEmpty) pf.lift(this.get) else None
//
//  @inline final def orElse[B >: A](alternative: => GenericOption[B]): GenericOption[B] =
//  if (isEmpty) alternative else this
//
//  def iterator: Iterator[A] =
//    if (isEmpty) collection.Iterator.empty else collection.Iterator.single(this.get)
//
//  def toList: List[A] =
//    if (isEmpty) List() else new ::(this.get, Nil)
//
//  @inline final def toRight[X](left: => X) =
//  if (isEmpty) Left(left) else Right(this.get)
//
//  @inline final def toLeft[X](right: => X) =
//  if (isEmpty) Right(right) else Left(this.get)
}

final case class GenericSome[A](t: A) extends GenericOption[A, GenericSome] {
  def isEmpty: Boolean = false

  def get: A = t

  def map[B](f: (A) => B): GenericSome[B] = GenericSome(f(t))
}


final case class GenericNone[A]() extends GenericOption[A, GenericNone] {
  def isEmpty: Boolean = true

  @SuppressWarnings(Array(Wart.Throw))
  def get: A = throw new NoSuchElementException("GenericNone.get")

  def map[B](f: (A) => B): GenericNone[B] = new GenericNone[B]()

  //is GenericNone[A] == GenericNone[B}, hope so
}
