package com.kristofszilagyi

import com.kristofszilagyi.Helpers._
import com.kristofszilagyi.shared.Wart.discard
import com.kristofszilagyi.shared.pixel.Pixel._
import japgolly.scalajs.react.vdom.TagOf
import japgolly.scalajs.react.vdom.svg_<^.^
import org.scalajs.dom.{Element, svg}
import japgolly.scalajs.react.vdom._
import japgolly.scalajs.react.vdom.svg_<^._
import org.scalajs.dom.svg._

object Helpers {
  trait Attr
  object Position extends Attr
  object Dimension extends Attr
  object Position1And2 extends Attr


  trait Has[AA <: Attr, T <: Element]
  def has[AA <: Attr, T <: Element]: Has[AA, T] = new Has[AA, T]{}

  trait HasDimensionAndPosition[T <: Element]
  def hasDimensionAndPosition[T <: Element]: HasDimensionAndPosition[T] = new HasDimensionAndPosition[T]{}
}


object TypeSafeAttributes {

  implicit def derivePosition[T <: Element](implicit hasDimensionAndPosition: HasDimensionAndPosition[T]): Has[Position.type , T] = {
    discard(hasDimensionAndPosition)
    has[Position.type, T]
  }
  implicit def deriveDimension[T <: Element](implicit hasDimensionAndPosition: HasDimensionAndPosition[T]): Has[Dimension.type , T] = {
    discard(hasDimensionAndPosition)
    has[Dimension.type, T]
  }

  //put the implicit here so intellj can tell what's on the method and what's not
  implicit class RichTagOf[T <: Element](tagOf: TagOf[T]) {
    def x(value: XPixel)(implicit ev: Has[Position.type, T]): TagOf[T] = {
      discard(ev)
      tagOf(^.x := value.d.toInt)
    }
    def y(value: YPixel)(implicit ev: Has[Position.type, T]): TagOf[T] = {
      discard(ev)
      tagOf(^.y := value.d.toInt)
    }

    def x1(value: XPixel)(implicit ev: Has[Position1And2.type, T]): TagOf[T] = {
      discard(ev)
      tagOf(^.x := value.d.toInt)
    }
    def y1(value: YPixel)(implicit ev: Has[Position1And2.type, T]): TagOf[T] = {
      discard(ev)
      tagOf(^.y := value.d.toInt)
    }

    def x2(value: XPixel)(implicit ev: Has[Position1And2.type, T]): TagOf[T] = {
      discard(ev)
      tagOf(^.x := value.d.toInt)
    }
    def y2(value: YPixel)(implicit ev: Has[Position1And2.type, T]): TagOf[T] = {
      discard(ev)
      tagOf(^.y := value.d.toInt)
    }


    def width(value: WPixel)(implicit ev: Has[Dimension.type, T]): TagOf[T] = {
      discard(ev)
      tagOf(^.width := value.d.toInt)
    }
    def height(value: HPixel)(implicit ev: Has[Dimension.type, T]): TagOf[T] = {
      discard(ev)
      tagOf(^.height := value.d.toInt)
    }
  }


  implicit val text2d: Has[Position.type, Text] = has[Position.type, Text]
  implicit val svg2d: HasDimensionAndPosition[SVG] = hasDimensionAndPosition[SVG]
  implicit val lineP12: Has[Position1And2.type, Line] = has[Position1And2.type, Line]
  implicit val rect: HasDimensionAndPosition[RectElement] = hasDimensionAndPosition[RectElement]
  //the react api is not typed enough so I need this
  val anythingPos: Has[Position.type, svg.Element] = has[Position.type, svg.Element]
}
