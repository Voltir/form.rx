package formidable

import scala.reflect.macros._
import scala.language.experimental.macros

object MacrosNext {
  def getCompanion(c: blackbox.Context)(tpe: c.Type) = {
    import c.universe._
    val symTab = c.universe.asInstanceOf[reflect.internal.SymbolTable]
    val pre = tpe.asInstanceOf[symTab.Type].prefix.asInstanceOf[Type]
    c.universe.internal.gen.mkAttributedRef(pre, tpe.typeSymbol.companion)
  }

  def generate[Layout: c.WeakTypeTag, Target: c.WeakTypeTag](c: blackbox.Context): c.Expr[Layout with FormidableRx[Target]] = {
    import c.universe._
    println("GENERATE START")
    val targetTpe = weakTypeTag[Target].tpe
    val layoutTpe = weakTypeTag[Layout].tpe
    targetTpe

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

    println("GENERATE AAAAA")
    val magic: List[c.Tree] = fields.zipWithIndex.map { case (field,idx) =>
      val term = TermName(s"a$idx")
      val accessor = layoutAccessors.find(_.name == field.name).get
      q"implicitly[BindRx[${accessor.info.dealias},${field.info.dealias}]].bind(this.$accessor,$term)"
    }

    val unmagic: List[c.Tree] = fields.map { case field =>
      println("GENERATE UNMAGIC: " + field)
      val accessor = layoutAccessors.find(_.name == field.name).get
      q"implicitly[BindRx[${accessor.info.dealias},${field.info.dealias}]].unbind(this.$accessor)().get"
    }

    val resetMagic: List[c.Tree] = fields.map { case field =>
      println("GENERATE RESET MAGIC: " + field)
      val accessor = layoutAccessors.find(_.name == field.name).get
      q"implicitly[BindRx[${accessor.info.dealias},${field.info.dealias}]].reset(this.$accessor)"
    }

    def bindN(n: Int) = {
      println("GENERATE BINDN")
      if(n > 0 && n < 23) {
        val vars = (0 until n).map(i => pq"${TermName(s"a$i")}")
        q"$companion.unapply(inp).map { case (..$vars) => $magic }"
      }
      else {
        c.abort(c.enclosingPosition,"Unsupported Case Class Dimension")
      }
    }

    def resetN(n: Int) = {
      println("GENERATE RESETN")
      if(n > 0 && n < 23) {
        val vars = (0 until n).map(i => pq"${TermName(s"a$i")}")
        q"$companion.unapply(inp).map { case (..$vars) => $magic }"
      }
      else {
        c.abort(c.enclosingPosition,"Unsupported Case Class Dimension")
      }
    }

    println("GENERATE OMG DONE")
    c.Expr[Layout with FormidableRx[Target]](q"""
      new $layoutTpe with FormidableRx[$targetTpe] {
        val current: rx.Rx[Try[$targetTpe]] = rx.Rx {
          Try {
            $companion.apply(..$unmagic)
          }
        }
        def set(inp: $targetTpe): Unit = {
          ${bindN(fields.size)}
        }

        def reset(): Unit = { ..$resetMagic }
      }
    """)
  }
}
