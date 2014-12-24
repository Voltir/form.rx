package formidable

import scala.reflect.macros._
import scala.reflect._
import scala.annotation.{ClassfileAnnotation, StaticAnnotation}
import scala.language.experimental.macros
object MacroTest {
  def paramsOf[T]: List[String] = macro Macros.paramsOf[T]
  def fwat[Layout,Target](layout: Layout): formidable.FormidableThisTimeBetter.Formidable[Target] = macro Macros.fwat[Layout,Target]
}

class key(s: String) extends StaticAnnotation

object Macros {

  def getCompanion(c: Context)(tpe: c.Type) = {
    import c.universe._
    val symTab = c.universe.asInstanceOf[reflect.internal.SymbolTable]
    val pre = tpe.asInstanceOf[symTab.Type].prefix.asInstanceOf[Type]
    c.universe.treeBuild.mkAttributedRef(pre, tpe.typeSymbol.companionSymbol)
  }

  def fwat[Layout: c.WeakTypeTag, Target: c.WeakTypeTag](c: Context)(layout: c.Expr[Layout])
    : c.Expr[formidable.FormidableThisTimeBetter.Formidable[Target]] = {
    import c.universe._

    val targetTpe = weakTypeTag[Target].tpe
    val layoutTpe = weakTypeTag[Layout].tpe

    //println(targetTpe)
    println("For layout...")
    //val layoutAccessors = layoutTpe.decls.collect{
    //  case x if x.isPublic && x.asTerm.isAccessor => { println(x); x.asTerm }
    //}.toList
    //println(layoutAccessors)
    //println("QQQQQQQQQQQQQQQQQQQQQQQQ")
    //println(targetTpe)
    val layoutAccessors = layoutTpe.decls.map(_.asTerm).filter(_.isAccessor).toList
    val targetAccessors = targetTpe.decls.map(_.asTerm).filter(_.isAccessor).toList
    println(targetAccessors)
    println(layoutAccessors)
    val layoutNames = layoutAccessors.map(_.name.toString).toSet
    val targetNames = targetAccessors.map(_.name.toString).toSet

    val missing = targetNames.diff(layoutNames)

    if(missing.size > 0) {

      c.abort(c.enclosingPosition,s"The layout is not fully defined: Missing fields are:\n${missing.mkString("\n")}")
    }
    println(targetTpe.typeSymbol.companion)
    val symTab = c.universe.asInstanceOf[reflect.internal.SymbolTable]
    println(symTab)
    println(s"???? ${targetTpe.asInstanceOf[symTab.Type]}")
    //val wurt = c.universe.internal.gen.mkAttributedRef(targetTpe,targetTpe.typeSymbol.companion)
    //println(wurt)
    //println(getCompanion(c)(targetTpe))
    c.Expr[formidable.FormidableThisTimeBetter.Formidable[Target]](q"""
    class LeL(binding: $layoutTpe) extends Formidable[$targetTpe]{
      def populate(inp: $targetTpe): Unit = {
        Thingz.unapply(inp).map { case (a1,a2) =>
          binding.${layoutAccessors(0)}.value = a1
          binding.${layoutAccessors(1)}.value = a2
          println("YOLOLOBITCHEZ")
        }
      }
    }
    new LeL($layout)
    """)
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

        val args = argSyms.map { _.name.toString }
        println(args)
        args
      }
    }
    c.Expr[List[String]](q"$wut")
  }
}
