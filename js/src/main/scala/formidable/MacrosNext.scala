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

    //Get subset of layout accessors that are rx.core.Var types
    val VAR_SYMBOL = typeOf[rx.core.Var[_]].typeSymbol
    val rxVarAccessors = layoutAccessors.filter { a =>
      a.typeSignature match {
        case NullaryMethodType(TypeRef(_,VAR_SYMBOL, _ :: Nil)) => true
        case _ => false
      }
    }

    val companion = getCompanion(c)(targetTpe)

    val magic: List[c.Tree] = fields.zipWithIndex.map { case (field,idx) =>
      val term = TermName(s"a$idx")
      val accessor = layoutAccessors.find(_.name == field.name).get
      q"implicitly[BindRx[${accessor.info.dealias},${field.info.dealias}]].bind(this.$accessor,$term,false)"
    }

    val unmagic: List[c.Tree] = fields.zipWithIndex.map { case (field,i) =>
      val accessor = layoutAccessors.find(_.name == field.name).get
      fq"${TermName(s"a$i")} <- implicitly[BindRx[${accessor.info.dealias},${field.info.dealias}]].unbind(this.$accessor)()"
    }

    val resetMagic: List[c.Tree] = fields.map { case field =>
      val accessor = layoutAccessors.find(_.name == field.name).get
      q"implicitly[BindRx[${accessor.info.dealias},${field.info.dealias}]].reset(this.$accessor)"
    }

    def bindN(n: Int) = {
      if(n > 0 && n < 23) {
        val vars = (0 until n).map(i => pq"${TermName(s"a$i")}")
        q"$companion.unapply(inp).map { case (..$vars) => $magic }"
      }
      else {
        c.abort(c.enclosingPosition,"Unsupported Case Class Dimension")
      }
    }

    def resetN(n: Int) = {
      if(n > 0 && n < 23) {
        val vars = (0 until n).map(i => pq"${TermName(s"a$i")}")
        q"$companion.unapply(inp).map { case (..$vars) => $magic }"
      }
      else {
        c.abort(c.enclosingPosition,"Unsupported Case Class Dimension")
      }
    }

    //Hack to make Var types resetable
    //Basic idea: Generate a Map that stores the initial values of Var from Layout
    //And on reset, use that map as default values for each Var
    val varMapMagic = rxVarAccessors.map { a =>
      val name = a.name.decodedName.toString
      q"$name -> this.$a.now"
    }

    val varUpdateMagic = rxVarAccessors.map { a =>
      val name = a.name.decodedName.toString
      val theType = a.typeSignature match {
        case NullaryMethodType(TypeRef(_,VAR_SYMBOL, mahtype :: Nil)) => mahtype.dealias
        case _ => c.abort(c.enclosingPosition,"Could not determine Var type!?")
      }
      q"this.$a.updateSilent(rxVarDefaults($name).asInstanceOf[$theType])"
    }

    c.Expr[Layout with FormidableRx[Target]](q"""
      new $layoutTpe with FormidableRx[$targetTpe] {

        private val rxVarDefaults: Map[String,Any] = Map(..$varMapMagic)

        private def resetVars(): Unit = { ..$varUpdateMagic }

        val current: rx.Rx[Try[$targetTpe]] = rx.Rx {
          for(..$unmagic) yield {
            $companion.apply(..${(0 until fields.size).map(i=>TermName("a"+i))})
          }
        }

        override def set(inp: $targetTpe, propagate: Boolean): Unit = {
          ${bindN(fields.size)}
          if(propagate) current.recalc()
        }

        def reset(): Unit = {
          resetVars()
         ..$resetMagic
         current.recalc()
        }
      }
    """)
  }
}
