package formidable

import scala.reflect.macros._
import scala.reflect._
import scala.annotation.{ClassfileAnnotation, StaticAnnotation}
import scala.language.experimental.macros
object MacroTest {
  def paramsOf[T]: List[String] = macro Macros.paramsOf[T]
}

class key(s: String) extends StaticAnnotation

object Macros {

  def getCompanion(c: Context)(tpe: c.Type) = {
    import c.universe._
    val symTab = c.universe.asInstanceOf[reflect.internal.SymbolTable]
    val pre = tpe.asInstanceOf[symTab.Type].prefix.asInstanceOf[Type]
    c.universe.treeBuild.mkAttributedRef(pre, tpe.typeSymbol.companionSymbol)
  }

  def customKey(c: Context)(sym: c.Symbol): Option[String] = {
    import c.universe._
    sym.annotations
      .find(_.tpe == typeOf[key])
      .flatMap(_.scalaArgs.headOption)
      .map{case Literal(Constant(s)) => s.toString}
  }

  def paramsOf[T: c.WeakTypeTag](c: Context): c.Expr[List[String]] = {
    import c.universe._
    val tpe = weakTypeTag[T].tpe
    println("########### TYPE SYMBOL ############")
    println(tpe.typeSymbol)
    println("########### TYPE ARGS ############")
    println(tpe.typeArgs)
    println("########### TYPE PARAMLIST ############")
    println(tpe.paramLists)
    println("########### TYPE CONSTRUCTOR ############")
    println(tpe.typeConstructor)
    println("##### clsSymbol???? #####")
    val clsSymbol = tpe.typeSymbol.asClass
    println(clsSymbol)

    val wut = tpe.declaration(nme.CONSTRUCTOR) match {
      case x => {
        println("Seriously, wat")
        println(s"Sealed? ${clsSymbol.isSealed}")
        println(s"isModuleClass? ${tpe.typeSymbol.isModuleClass}")
        println(x)
        println("-------------------------------------")
        val companion = getCompanion(c)(tpe)
        println(companion)
        val argSyms = companion
          .tpe
          .member(newTermName("apply"))
          .asMethod
          .paramLists
          .flatten

        val args = argSyms.map { p =>
          customKey(c)(p).getOrElse(p.name.toString)
        }
        println(args)
        args
      }
    }
    c.Expr[List[String]](q"$wut")
  }
}
