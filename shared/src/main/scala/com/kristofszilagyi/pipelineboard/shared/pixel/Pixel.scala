package com.kristofszilagyi.pipelineboard.shared.pixel

import com.kristofszilagyi.pipelineboard.shared.Wart._
import com.kristofszilagyi.pipelineboard.shared.pixel.Pixel._

object Pixel {
  sealed trait PixelType
  object X extends PixelType
  object Y extends PixelType
  object Width extends PixelType
  object Height extends PixelType


  type XPixel = Pixel[X.type]
  type YPixel = Pixel[Y.type]
  type WPixel = Pixel[Width.type]
  type HPixel = Pixel[Height.type]

  implicit class RichInt(i: Int) {
    def wpx: WPixel = Pixel[Width.type](i.toDouble)
    def xpx: XPixel = Pixel[X.type](i.toDouble)
    def ypx: YPixel = Pixel[Y.type](i.toDouble)
    def hpx: HPixel = Pixel[Height.type](i.toDouble)
  }

  implicit class RichDouble(d: Double) {
    def wpx: WPixel = Pixel[Width.type](d)
    def xpx: XPixel = Pixel[X.type](d)
    def ypx: YPixel = Pixel[Y.type](d)
    def hpx: HPixel = Pixel[Height.type](d)
  }

  implicit class RichX(p: Pixel[X.type]) {
    def toW: Pixel[Width.type] = Pixel[Width.type](p.d)
  }
  implicit class RichY(p: Pixel[Y.type]) {
    def toH: Pixel[Height.type] = Pixel[Height.type](p.d)
  }
  implicit class RichW(p: Pixel[Width.type]) {
    def toX: Pixel[X.type] = Pixel[X.type](p.d)
  }
  implicit class RichH(p: Pixel[Height.type]) {
    def toY: Pixel[Y.type] = Pixel[Y.type](p.d)
  }

}
@SuppressWarnings(Array(Overloading))
final case class Pixel[P <: PixelType](d: Double) {
  def +(other: Pixel[P]): Pixel[P] = Pixel[P](d + other.d)

  def -(other: Pixel[P]): Pixel[P] = Pixel[P](d - other.d)

  def max(other: Pixel[P]): Pixel[P] = {
    if (d > other.d) this
    else other
  }

  def *(other: Int): Pixel[P] = Pixel[P](d * other)
  def *(other: Double): Pixel[P] = Pixel[P](d * other)
  def /(other: Int): Pixel[P] = Pixel[P](d / other)
  def /(other: Double): Pixel[P] = Pixel[P](d / other)

}