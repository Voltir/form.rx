package formrx.implicits

import formrx._
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

  class CheckboxBoolRx(default: Boolean)(mods: Modifier *)(implicit ctx: Ctx.Owner) extends FormRx[Boolean] {
    val input = scalatags.JsDom.all.input(`type`:="checkbox", mods).render
    val current: rx.Rx[Try[Boolean]] = rx.Rx { Try(input.checked) }

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
  class CheckboxBase[T, Container[_] <: Traversable[_]]
      (name: String)
      (buildFrom: Seq[T] => Container[T], hasValue: Container[T] => T => Boolean)
      (checks: Chk[T] *)
      (implicit ctx: Ctx.Owner) extends FormRx[Container[T]] {

    override val current: Rx[Try[Container[T]]] = Rx { Try { buildFrom(currentlyChecked()) }}

    val checkboxes = checks.map { c =>
      c.input.name = name
      c.input.onchange = { (_:Event) => current.recalc() }
      c }.toBuffer

    private def currentlyChecked(): Seq[T] = {
      checks.filter(_.input.checked).map(_.value)
    }

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

  class DynamicCheckbox[T, Container[_] <: Traversable[_]]
      (name: String)
      (buildFrom: Seq[T] => Container[T], hasValue: Container[T] => T => Boolean)
      (checksRx: rx.Rx[List[Chk[T]]])
      (implicit ctx: Ctx.Owner) extends FormRx[Container[T]] {

    val current: Rx[Try[Container[T]]] = checksRx.map { cs =>
      Try(buildFrom(cs.filter(_.input.checked).map(_.value)))
    }

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

    private val watchChecks = checksRx.foreach { values =>
      values.foreach { chk =>
        chk.input.name = name
        chk.input.onchange = { (_: Event) => current.recalc() }
      }
    }
  }

  object CheckboxRx {
    def bool(default: Boolean, modifiers: Modifier *)(implicit ctx: Ctx.Owner) =
      new CheckboxBoolRx(default)(modifiers)

    def set[T](name: String)(checks: Chk[T] *)(implicit ctx: Ctx.Owner) =
      new CheckboxBase[T,Set](name)(_.toSet, c => v => c.contains(v))(checks:_*)

    def list[T](name: String)(checks: Chk[T] *)(implicit ctx: Ctx.Owner) =
      new CheckboxBase[T,List](name)(_.toList, c => v => c.contains(v))(checks:_*)

    def dynamicSet[T](name: String)(checks: rx.Rx[List[Chk[T]]])(implicit ctx: Ctx.Owner) =
      new DynamicCheckbox[T, Set](name)(_.toSet, c => v => c.contains(v))(checks)
  }
}
