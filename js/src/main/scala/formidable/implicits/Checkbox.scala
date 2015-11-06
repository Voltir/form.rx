package formidable.implicits

import formidable._
import org.scalajs.dom._
import scala.util.Try
import scalatags.JsDom.all._
import rx._

trait Checkbox {

  //Wrapper for checkbox type inputs
  class Chk[+T](val value: T)(mods: Modifier *) {
    val input = scalatags.JsDom.all.input(`type`:="checkbox",mods).render
  }

  object Chk {
    def apply[T](value: T)(mods: Modifier *) = new Chk(value)(mods)
  }

  class CheckboxBoolRx(default: Boolean)(mods: Modifier *) extends FormidableRx[Boolean] {
    val input = scalatags.JsDom.all.input(`type`:="checkbox", mods).render
    val current: rx.Rx[Try[Boolean]] = rx.Rx { Try(input.checked)}

    override def set(inp: Boolean): Unit = {
      input.checked = inp
      current.recalc()
    }

    override def reset(): Unit = {
      set(default)
    }

    input.onchange = { (_:Event) => current.recalc() }
  }

  //BindRx for Set[T]/List[T] <=> Checkbox elements
  class CheckboxBaseRx[T, Container[_] <: Traversable[_]]
      (name: String)
      (buildFrom: Seq[T] => Container[T], hasValue: Container[T] => T => Boolean)
      (checks: Chk[T] *) extends FormidableRx[Container[T]] {
    val checkboxes = checks.map { c =>
      c.input.name = name
      c.input.onchange = { (_:Event) => current.recalc() }
      c }.toBuffer


    private def currentlyChecked(): Seq[T] = {
      checks.filter(_.input.checked).map(_.value)
    }

    private var wat = 0
    override val current: Rx[Try[Container[T]]] = Rx { wat += 1 ; Try { buildFrom(currentlyChecked()) }}

    override def set(values: Container[T]) = {
      val (checked,unchecked) = checks.partition(c => hasValue(values)(c.value))
      checked.foreach   { _.input.checked = true  }
      unchecked.foreach { _.input.checked = false }
      current.recalc()
    }

    override def reset(): Unit = {
      checks.foreach { _.input.checked = false }
      current.recalc()
    }
  }

  class DynamicCheckboxRx[T, Container[_] <: Traversable[_]]
      (name: String)
      (buildFrom: Seq[T] => Container[T], hasValue: Container[T] => T => Boolean)
      (checksRx: Rx[List[Chk[T]]])
      extends FormidableRx[Container[T]] {

    private def currentlyChecked(): Seq[T] = {
      checksRx().filter(_.input.checked).map(_.value)
    }

    val current: Rx[Try[Container[T]]] = Rx{println("current rxing"); Try{buildFrom(currentlyChecked())}}

    override def set(values: Container[T]) = {
      val (checked, unchecked) = checksRx.now.partition(c => hasValue(values)(c.value))
      checked.foreach   { _.input.checked = true  }
      unchecked.foreach { _.input.checked = false }
      current.recalc()
    }

    override def reset(): Unit = {
      checksRx.now.foreach { _.input.checked = false }
      current.recalc()
    }

    private val watchChecks = Obs(checksRx) {
      checksRx.now.map { chk =>
        chk.input.name = name
        chk.input.onchange = { (_: Event) => current.recalc() }
      }
    }
  }

  object CheckboxRx {
    def bool(default: Boolean, modifiers: Modifier *) = new CheckboxBoolRx(default)(modifiers)
    def set[T](name: String)(checks: Chk[T] *)    = new CheckboxBaseRx[T,Set](name)(_.toSet, c => v => c.contains(v))(checks:_*)
    def list[T](name: String)(checks: Chk[T] *)   = new CheckboxBaseRx[T,List](name)(_.toList, c => v => c.contains(v))(checks:_*)
    def dynamicSet[T](name: String)(checks: Rx[List[Chk[T]]]) =
      new DynamicCheckboxRx[T, Set](name)(_.toSet, c => v => c.contains(v))(checks)
  }
}
