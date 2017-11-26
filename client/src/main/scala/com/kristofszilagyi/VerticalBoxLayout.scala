package com.kristofszilagyi

import com.kristofszilagyi.shared.pixel.Pixel._
import japgolly.scalajs.react.vdom.TagOf
import japgolly.scalajs.react.vdom.svg_<^._
import org.scalajs.dom.raw.SVGElement
import TypeSafeAttributes._

final case class ElementWithHeight(element: TagOf[SVGElement], height: HPixel)
final case class ElementWithHeightAndPosition(element: TagOf[SVGElement], height: HPixel, y: YPixel)


object VerticalBoxLayout {
  def arrange(elements: Seq[ElementWithHeight]): Seq[TagOf[SVGElement]] = {
    val elementsWithPositions = elements.foldLeft(Seq.empty[ElementWithHeightAndPosition]) { case (acc, elementWithHeight) =>
      val newElement = ElementWithHeightAndPosition(elementWithHeight.element, elementWithHeight.height,
        acc.aggregate(0.hpx)(_ + _.height, _ + _).toY)
      acc :+ newElement
    }
    elementsWithPositions.map { element =>
      <.svg(element.element)
        .height(element.height)
        .y(element.y)
    }
  }
}
