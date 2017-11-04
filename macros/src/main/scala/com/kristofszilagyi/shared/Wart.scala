package com.kristofszilagyi.shared

@SuppressWarnings(Array("org.wartremover.warts.FinalVal", "org.wartremover.warts.PublicInference"))
object Wart {
  final val FinalVal = "org.wartremover.warts.FinalVal"
  final val Public = "org.wartremover.warts.PublicInference"
  final val Var = "org.wartremover.warts.Var"

  final val Equals = "org.wartremover.warts.Equals"
  final val AsInstanceOf = "org.wartremover.warts.AsInstanceOf"
  final val ToString = "org.wartremover.warts.ToString"
  final val Null = "org.wartremover.warts.Null"
  final val Product = "org.wartremover.warts.Product"
  final val Serializable = "org.wartremover.warts.Serializable"
  final val EitherProjectionPartial = "org.wartremover.warts.EitherProjectionPartial"
  final val Throw = "org.wartremover.warts.Throw"
  final val Recursion = "org.wartremover.warts.Recursion"
  final val TraversableOps = "org.wartremover.warts.TraversableOps"
  final val Overloading = "org.wartremover.warts.Overloading"
  final val DefaultArguments = "org.wartremover.warts.DefaultArguments"
  final val StringPlusAny = "org.wartremover.warts.StringPlusAny"
  final val OptionPartial = "org.wartremover.warts.OptionPartial"

  def discard(a: Any): Unit = {
    val _ = a
  }
}
