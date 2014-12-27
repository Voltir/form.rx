package formidable

import scala.reflect.macros._
import scala.language.experimental.macros
import scala.reflect._
import scala.annotation.{ClassfileAnnotation, StaticAnnotation}

object MacroTest {
  def paramsOf[T](): List[String] = macro Macros.paramsOf[T]
  def fwat[Layout,Target](layout: Layout): formidable.FormidableThisTimeBetter.Formidable[Target] = macro Macros.mkFormidable[Layout,Target]
}

object Macros {

  def getCompanion(c: blackbox.Context)(tpe: c.Type) = {
    import c.universe._
    val symTab = c.universe.asInstanceOf[reflect.internal.SymbolTable]
    val pre = tpe.asInstanceOf[symTab.Type].prefix.asInstanceOf[Type]
    c.universe.internal.gen.mkAttributedRef(pre, tpe.typeSymbol.companion)
  }

  def mkFormidable[Layout: c.WeakTypeTag, Target: c.WeakTypeTag](c: blackbox.Context)(layout: c.Expr[Layout])
    : c.Expr[formidable.FormidableThisTimeBetter.Formidable[Target]] = {
    import c.universe._

    val targetTpe = weakTypeTag[Target].tpe
    val layoutTpe = weakTypeTag[Layout].tpe

    val fields = targetTpe.decls.collectFirst {
      case m: MethodSymbol if m.isPrimaryConstructor => m
    }.get.paramLists.head

    val layoutAccessors = layoutTpe.decls.map(_.asTerm).filter(_.isAccessor).toList
    val layoutNames = layoutAccessors.map(_.name.toString).toSet
    val targetNames = fields.map(_.name.toString).toSet
    val missing = targetNames.diff(layoutNames)

    if(missing.size > 0) {
      c.abort(c.enclosingPosition,s"The layout is not fully defined: Missing fields are:\n${missing.mkString("\n")}")
    }

    val companion = getCompanion(c)(targetTpe)

    val magic = fields.zipWithIndex.map { case (field,idx) =>
      val term = TermName(s"a$idx")
      q"$layout.${layoutAccessors.find(_.name == field.name).get}.value = $term"
    }

    val bnd2 = q"$companion.unapply(inp).map { case (a0,a1) => $magic }"
    val bnd3 = q"$companion.unapply(inp).map { case (a0,a1,a2) => $magic }"
    val bnd4 = q"$companion.unapply(inp).map { case (a0,a1,a2,a3) => $magic }"
    val bnd5 = q"$companion.unapply(inp).map { case (a0,a1,a2,a3,a4) => $magic }"
    val bnd6 = q"$companion.unapply(inp).map { case (a0,a1,a2,a3,a4,a5) => $magic }"

    c.Expr[formidable.FormidableThisTimeBetter.Formidable[Target]](q"""
    new Formidable[$targetTpe] {
      def populate(inp: $targetTpe): Unit = {
        ${ fields.size match {
          case 2 => bnd2
          case 3 => bnd3
          case 4 => bnd4
          case 5 => bnd5
          case 6 => bnd6
          case _ => c.abort(c.enclosingPosition,"Unsupported Case Class Dimension")
        }}
      }

      def construct(): $targetTpe = {
        $companion.apply(
          $layout.${layoutAccessors(0)}.value,
          $layout.${layoutAccessors(1)}.value,
          $layout.${layoutAccessors(2)}.value
        )
      }
    }
    """)
  }

  def paramsOf[T: c.WeakTypeTag](c: whitebox.Context)(): c.Expr[List[String]] = {
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
    println("#########  TERM SYMBOL ###########")
    println(tpe.termSymbol)
    println("#########  FUCKING FUCK FUCK ###########")
    println(tpe.typeSymbol.isClass)
    println(tpe.typeSymbol.companionSymbol)

    val wut = tpe.decl(termNames.CONSTRUCTOR) match {
      case x => {
        println("Seriously, wat")
        println(s"Sealed? ${clsSymbol.isSealed}")
        println(s"isModuleClass? ${tpe.typeSymbol.isModuleClass}")
        println("-------------------------------------")
        val companion = getCompanion(c)(tpe)
        val argSyms = companion
          .tpe
          .member(TermName("apply"))
          .asMethod
          .paramLists
          .flatten

        val args = argSyms.map { _.name.toString }
        println(args)
        args
      }
    }
    c.Expr[List[String]](q"$wut")
  }
}
