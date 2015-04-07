package formidable

import formidable.Implicits.{Chk, Radio, Opt}
import org.scalajs.dom
import scala.util.{Success, Try}

object ImplicitsNext {
  import org.scalajs.dom._
  import scalatags.JsDom.all.{html => _, _}

  //Implicit for general FormidableRx
  class FormidableBindRx[F <: FormidableRx[Target],Target] extends BindRx[F,Target] {
    override def bind(inp: F, value: Target) = inp.unbuild(value)
    override def unbind(inp: F): rx.Rx[Try[Target]] = inp.current
  }

  implicit def implicitFormidableBindRx[F <: FormidableRx[Target],Target]: BindRx[F,Target] = new FormidableBindRx[F,Target]


  class BindVarRx[T]() extends BindRx[rx.Var[T],T] {
    override def bind(inp: rx.Var[T], value: T) = inp() = value
    override def unbind(inp: rx.Var[T]): rx.Rx[Try[T]] = rx.Rx { Try(inp()) }
  }
  implicit def implicitBindVarRx[Target]: BindRx[rx.Var[Target],Target] = new BindVarRx[Target]()


  //Binders for T <=> Select element
  class SelectionRx[T](selectMods: Modifier *)(options: Opt[T] *) extends FormidableRx[T] {
    private val values = options.map(_.value).toBuffer
    private val selected: rx.Var[T] = rx.Var(options.head.value)

    val select: dom.html.Select = scalatags.JsDom.all.select(
      scalatags.JsDom.all.onchange:={() => selected() = values(select.selectedIndex) }
    )(selectMods)(options.map(_.option):_*).render

    val current: rx.Rx[Try[T]] = rx.Rx {
      Try(selected())
    }

    override def unbuild(value: T): Unit = {
      select.selectedIndex = options.indexWhere(_.value == value)
      selected() = value
    }
  }

  object SelectionRx {
    def apply[T](selectMods: Modifier*)(options: Opt[T] *) = new SelectionRx[T](selectMods)(options:_*)
    //def mapped[T,U](func: T => U, selectMods: Modifier*)(options: Opt[T] *) = new SelectionRx[T](selectMods)(options:_*).mapped(func)
  }

  //Binders for T <=> Radio elements
  class RadioRx[T](name: String)(val radios: Radio[T] *) extends FormidableRx[T] {
    private val selected: rx.Var[T] = rx.Var(radios.head.value)
    radios.foreach(_.input.name = name)

    val current: rx.Rx[Try[T]] = rx.Rx { Try(selected()) }

    def unbuild(value: T): Unit = radios.find(_.value == value).foreach { r =>
      r.input.checked = true
      selected() = r.value
    }
  }

  object RadioRx {
    def apply[T](name: String)(radios: Radio[T] *) = new RadioRx[T](name)(radios:_*)
  }

  //Binders for Boolean <=> checkbox
//  implicit object CheckboxBoolRx extends BindRx[CheckboxBool,Boolean] {
//    protected def ensureShove(inp: dom.html.Input): rx.Rx[Try[Boolean]] = {
//      if(inp.asInstanceOf[js.Dynamic].selectDynamic("_inp_rx") == js.undefined) {
//        val result: rx.Rx[Try[Boolean]] = rx.Rx { println("BOOL RX WTF!") ; Success(inp.checked) }
//        inp.asInstanceOf[js.Dynamic].updateDynamic("_inp_rx")(result.asInstanceOf[js.Any])
//        result
//      } else {
//        inp.asInstanceOf[js.Dynamic]._inp_rx.asInstanceOf[rx.Rx[Try[Boolean]]]
//      }
//    }
//    override def bind(inp: CheckboxBool, value: Boolean): Unit = {
//      println("BIND CHECKBOX BOOL PLZ")
//      inp.input.checked = value
//      ensureShove(inp.input).recalc()
//    }
//
//    override def unbind(inp: CheckboxBool): rx.Rx[Try[Boolean]] = {
//      println("UNBIND CHECKBOX BOOL PLZ")
//      val result = ensureShove(inp.input)
//      inp.input.onchange = (ev:dom.Event) => result.recalc()
//      result
//    }
//  }

  class CheckboxBoolRx(mods: Modifier *) extends FormidableRx[Boolean] {

    val input = scalatags.JsDom.all.input(`type`:="checkbox", mods).render

    val current: rx.Rx[Try[Boolean]] = rx.Rx { Try(input.checked)}

    override def unbuild(inp: Boolean): Unit = {
      input.checked = inp
      current.recalc()
    }

    input.onchange = { (_:Event) => current.recalc() }


  }




  //BindRx for Set[T]/List[T] <=> Checkbox elements
  class CheckboxBaseRx[T, Container[_]](name: String)(buildFrom: Seq[T] => Container[T], hasValue: Container[T] => T => Boolean)(checks: Chk[T] *) extends FormidableRx[Container[T]] {
    val checkboxes = checks.map { c =>
      c.input.name = name
      c.input.onchange = { (_:Event) => current.recalc() }
      c }.toBuffer

    lazy val current: rx.Rx[Try[Container[T]]] = rx.Rx { Try {
      buildFrom(checks.filter(_.input.checked).map(_.value))
    }}

    override def unbuild(values: Container[T]) = {
      val (checked,unchecked) = checks.partition(c => hasValue(values)(c.value))
      checked.foreach   { _.input.checked = true  }
      unchecked.foreach { _.input.checked = false }
      current.recalc()
    }
  }

  object CheckboxRx {
    def set[T](name: String)(checks: Chk[T] *)    = new CheckboxBaseRx[T,Set](name)(_.toSet, c => v => c.contains(v))(checks:_*)
    def list[T](name: String)(checks: Chk[T] *)   = new CheckboxBaseRx[T,List](name)(_.toList, c => v => c.contains(v))(checks:_*)
  }

}