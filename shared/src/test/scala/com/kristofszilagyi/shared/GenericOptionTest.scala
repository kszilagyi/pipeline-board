package com.kristofszilagyi.shared

import utest._
import utest.framework.{Test, Tree}

import scala.language.higherKinds

object GenericOptionTest extends TestSuite {

  def algo[O[T] <: GenericOption[T, O]](o: O[Int]): O[Int] = {
    o.map(i => i + 1)
  } //intellJ complains but it compiles!

  def tests: Tree[Test] = this {
    'test1 {
      algo(GenericNone[Int]()) ==> GenericNone()
    }

    'test2 {
      algo(GenericSome[Int](1)) ==> GenericSome[Int](2)
    }

    'test3 {
      println(algo(GenericSome[Int](1)).t) //Wow

    }


  }
}
