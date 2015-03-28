package formidable

import formidable.Implicits.{Chk, CheckboxBool, Radio, Opt}
import org.scalajs.dom
import org.scalajs.dom.html
import scala.scalajs.js
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
    override def unbind(inp: rx.Var[T]): rx.Rx[Try[T]] = rx.Rx { inp() ; inp.toTry }
  }
  implicit def implicitBindVarRx[Target] = new BindVarRx[Target]()

  //Helper trait to shove Rx[Try[T]] into a dom.html.Input
  trait InputRxShove[T] {
    protected def ensureShove(inp: html.Input)(make: String => Try[T]): rx.Rx[Try[T]] = {
      if(inp.asInstanceOf[js.Dynamic].selectDynamic("_inp_rx") == js.undefined) {
        val result: rx.Rx[Try[T]] = rx.Rx {
          println("InputBindRx -- I AM WORRIED THIS LEAKS!")
          make(inp.value)
        }
        inp.asInstanceOf[js.Dynamic].updateDynamic("_inp_rx")(result.asInstanceOf[js.Any])
        result
      } else {
        inp.asInstanceOf[js.Dynamic]._inp_rx.asInstanceOf[rx.Rx[Try[T]]]
      }
    }
  }

  //Binder for HTMLInputElement
  implicit object InputBindRx extends BindRx[html.Input,String] with InputRxShove[String] {

    def bind(inp: html.Input, value: String): Unit = {
      inp.value = value
      ensureShove(inp)(s => Success(inp.value)).recalc()
    }

    def unbind(inp: html.Input): rx.Rx[Try[String]] = {
      val result = ensureShove(inp)(s => Success(inp.value))
      inp.onkeyup = (ev:dom.KeyboardEvent) => {
        println("InputBindRx -- RECALC!")
        result.recalc()
      }
      result
    }
  }

  //Binder for HTMLInputElement
  class InputNumericBindRx[N: Numeric](make: String => Try[N]) extends BindRx[html.Input,N] with InputRxShove[N]{

    override def bind(inp: html.Input, value: N): Unit = {
      inp.value = value.toString
      ensureShove(inp)(make).recalc()
    }

    override def unbind(inp: html.Input): rx.Rx[Try[N]] = {
      val result = ensureShove(inp)(make)
      inp.onkeyup = (ev:dom.KeyboardEvent) => {
        println("InputBindRx -- RECALC!")
        result.recalc()
      }
      result
    }
  }

  implicit val InputIntBinder = new InputNumericBindRx[Int](s => Try(s.toInt))
  implicit val InputLongBinder = new InputNumericBindRx[Long](s => Try(s.toLong))
  implicit val InputFloatBinder = new InputNumericBindRx[Float](s => Try(s.toFloat))
  implicit val InputDoubleBinder = new InputNumericBindRx[Double](s => Try(s.toDouble))

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
  implicit object CheckboxBoolRx extends BindRx[CheckboxBool,Boolean] {
    protected def ensureShove(inp: html.Input): rx.Rx[Try[Boolean]] = {
      if(inp.asInstanceOf[js.Dynamic].selectDynamic("_inp_rx") == js.undefined) {
        val result: rx.Rx[Try[Boolean]] = rx.Rx {
          Success(inp.checked)
        }
        inp.asInstanceOf[js.Dynamic].updateDynamic("_inp_rx")(result.asInstanceOf[js.Any])
        result
      } else {
        inp.asInstanceOf[js.Dynamic]._inp_rx.asInstanceOf[rx.Rx[Try[Boolean]]]
      }
    }
    override def bind(inp: CheckboxBool, value: Boolean): Unit = {
      inp.input.checked = value
      ensureShove(inp.input).recalc()
    }
    override def unbind(inp: CheckboxBool): rx.Rx[Try[Boolean]] = ensureShove(inp.input)
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
