package com.kristofszilagyi

object Pixel {
  implicit class RichInt(i: Int) {
    def px: Pixel = Pixel(i)
  }
}
final case class Pixel(i: Int) {
  def +(other: Pixel): Pixel = Pixel(i + other.i)

  def -(other: Pixel): Pixel = Pixel(i - other.i)

  def *(other: Int): Pixel = Pixel(i * other)
  def /(other: Int): Pixel = Pixel(i / other)

}
