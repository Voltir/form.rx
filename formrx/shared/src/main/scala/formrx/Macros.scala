package formrx

import scala.reflect.macros._
import scala.language.experimental.macros

object Macros {

  def getCompanion(c: blackbox.Context)(tpe: c.Type) = {
    import c.universe._
    val symTab = c.universe.asInstanceOf[reflect.internal.SymbolTable]
    val pre = tpe.asInstanceOf[symTab.Type].prefix.asInstanceOf[Type]
    c.universe.internal.gen.mkAttributedRef(pre, tpe.typeSymbol.companion)
  }

  def generate[T: c.WeakTypeTag, Layout: c.WeakTypeTag](c: blackbox.Context)(ctx: c.Expr[rx.Ctx.Owner]): c.Expr[Layout with FormRx[T] with Procs] = {
    import c.universe._
    val ownerTpe = weakTypeOf[rx.Ctx.Owner]
    val layoutTpe = weakTypeTag[Layout].tpe
    val targetTpe = weakTypeTag[T].tpe


    //Ensure Layout can be constructed in the expected way
    val isValidLayout = layoutTpe.decls.exists {
      case m: MethodSymbol if m.isPrimaryConstructor =>
        m.paramLists match {
          case List(Nil,List(owner)) if owner.asTerm.typeSignature.weak_<:<(ownerTpe) => true
          case _ => false
        }
      case _ => false
    }

    if(!isValidLayout) {
      val msg = s"Invalid Layout: requires a constructor of the from $layoutTpe()(implicit ctx: Ctx.Owner)!"
      c.abort(c.enclosingPosition,msg)
    }

    //Collect all fields for the target type
    val fields = targetTpe.decls.collectFirst {
      case m: MethodSymbol if m.isPrimaryConstructor => m
    }.get.paramLists.head

    val layoutAccessors = layoutTpe.decls.map(_.asTerm).filter(_.isAccessor).toList
    val layoutNames = layoutAccessors.map(_.name.toString).toSet
    val targetNames = fields.map(_.name.toString).toSet
    val missing = targetNames.diff(layoutNames)
    if(missing.nonEmpty) {
      c.abort(c.enclosingPosition,s"The layout is not fully defined: Missing fields are:\n--- ${missing.mkString("\n--- ")}")
    }

    //Get subset of layout accessors that are rx.core.Var types
    val VAR_SYMBOL = typeOf[rx.Var[_]].typeSymbol
    val rxVarAccessors = layoutAccessors.filter { a =>
      a.typeSignature match {
        case NullaryMethodType(TypeRef(_,VAR_SYMBOL, _ :: Nil)) => true
        case _ => false
      }
    }

    val procAccessors = layoutAccessors.filter { a =>
      a.typeSignature match {
        case NullaryMethodType(tpe) if tpe <:< typeOf[formrx.Procs] => true
        case _ => false
      }
    }

    val companion = getCompanion(c)(targetTpe)

    val magic: List[c.Tree] = fields.zipWithIndex.map { case (field,idx) =>
      val term = TermName(s"a$idx")
      val accessor = layoutAccessors.find(_.name == field.name).get
      q"implicitly[BindRx[${accessor.info.dealias},${field.info.dealias}]].bind(this.$accessor,$term)"
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
        q"""$companion.unapply(inp).map { case (..$vars) => $magic }"""
      }
      else {
        c.abort(c.enclosingPosition,"Unsupported Case Class Dimension")
      }
    }

    //Hack to make Var types resetable
    //Basic idea: Generate vals that store the default Var value when constructed
    //And on reset, use those defaults vals to reset each Var
    val varDefaultsMagic = rxVarAccessors.map { a =>
      val default = a.name.decodedName.toString + "Default"
      q"val ${TermName(default)} = this.$a.now"
    }

    def varResetMagic: Option[c.Tree] = {
      val defaults = rxVarAccessors.map { a =>
        val default = a.name.decodedName.toString + "Default"
        q"rx.VarTuple(this.$a,${TermName(default)})"
      }
      if(defaults.nonEmpty) Option(q"rx.Var.set(..$defaults)") else None
    }

    val procMagic =
      if(procAccessors.nonEmpty) {
        val procs = procAccessors.map(p => q"this.$p.proc()")
        q"override def proc(): Unit = { ..$procs }"
      } else {
        q"override def proc(): Unit = ()"
      }


    c.Expr[Layout with FormRx[T] with Procs](q"""
      new $layoutTpe()($ctx) with formrx.FormRx[$targetTpe] with formrx.Procs {
        implicit val ctx: rx.Ctx.Owner = $ctx

        private var isUpdating: Boolean = false

        ..$varDefaultsMagic

        override val current: rx.Rx[scala.util.Try[$targetTpe]] = rx.Rx {
          if(isUpdating) {
            scala.util.Failure(formrx.FormidableProcessingFailure)
          } else {
            for(..$unmagic) yield {
              $companion.apply(..${fields.indices.map(i=>TermName("a"+i))})
            }
          }
        }

        override def set(inp: $targetTpe): Unit = {
          isUpdating = true
          ${bindN(fields.size)}
          isUpdating = false
          current.recalc()
        }

        override def reset(): Unit = {
          isUpdating = true
          $varResetMagic
          ..$resetMagic
          isUpdating = false
          current.recalc()
        }

        $procMagic
      }
    """)
  }
}
