package com.kristofszilagyi.shared.macros


import scala.reflect.macros.whitebox.Context
import scala.language.experimental.macros
import scala.annotation.StaticAnnotation
import scala.reflect.internal.Mode
import com.kristofszilagyi.shared.TypeSafeEqualsOps._
import com.kristofszilagyi.shared.Wart

@SuppressWarnings(Array(Wart.ToString, Wart.TraversableOps))
object AutoWrapper {

  def impl(c: Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._
    import Flag._
    val result = {
      annottees.map(_.tree).toList match {
        case q"final case class $name(self: $innerTypeName) extends ..$parents { ..$body }" :: Nil =>
          val innerType = c.typecheck(innerTypeName, c.TYPEmode).tpe
            val result = q"""
            final case class $name(self: $innerTypeName) extends ..$parents {
              ..${
                // TODO better filtering - allow overriding/overloading
                val existingDirectMethods = body.flatMap(tree => tree match {
                  case DefDef(_, n, _, _, _, _) => Some(n).toList
                  case _ => None.toList
                }).toSet

                println(s"parents: $parents")

                val inheritedMethods = (parents :+ tq"Object").flatMap { tpe =>
                  c.typecheck(tpe, c.TYPEmode).tpe.members
                }

                val existingMethods = inheritedMethods.map(_.name) ++ existingDirectMethods
                innerType.members.filter(innerMember => !existingMethods.contains(innerMember.name.toTermName)).map { member =>
                  val memberName = member.name.toTermName
                  val memberResult = member.typeSignature.resultType

                  val parameters = member.typeSignature.paramLists
                  println(member)
                  val (result, impl) =
                    if (memberResult ==== innerType) {
                      (Ident(TypeName(name.toString)),  q"""${name.toString}(self.$memberName)""")
                    } else {
                      (TypeTree(memberResult), q"""self.$memberName""")
                    }
                  if(parameters.nonEmpty && parameters.head.nonEmpty) {
                    println(parameters)
                   // val params = parameters.map(_.map(p => q"""${p.name.toTermName}: ${p.typeSignature}"""))
                    //val paramsHead = q"""..${params.head}""" //todo does not handle currying
                    val paramsHead = parameters.map(_.map {
                      paramSymbol => ValDef(
                        Modifiers(Flag.PARAM, typeNames.EMPTY, List()),
                        paramSymbol.name.toTermName,
                        TypeTree(paramSymbol.typeSignature),
                        rhs = EmptyTree)
                    }).head
                    print(showRaw(paramsHead))
                    print(paramsHead)
                    val passingParamsHead = parameters.map(_.map(_.name)).head
                    q"""def $memberName(..$paramsHead): $result = $impl(..$passingParamsHead)"""
                  } else {
                    q"""def $memberName: $result = $impl"""
                  }
                }
              }
              ..$body
            }
          """
          println(result)
          result
        case _ => c.abort(c.enclosingPosition, "You have to annotate a final case class which have one self field")
      }
    }
    c.Expr[Any](result)
  }
}

class AutoWrapper extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro AutoWrapper.impl
}