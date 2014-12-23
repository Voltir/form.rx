package formidable

import org.scalajs.dom
import rx._
import rx.ops._
import scalatags.JsDom.Modifier
import scalatags.JsDom.all._

trait Validator[V,T] {

  def validate(inp: V): Boolean

  protected def build(inp: V): T

  def unbuild(inp: T): V

  def create(inp: V): T = {
    if(validate(inp)) build(inp)
    else throw new IllegalArgumentException
  }
}

trait Field[T] {
  def isValid: Var[Boolean]
  def field: dom.HTMLElement
  def populate(inp: T): Unit
  def construct(): T
}

class InputField[T]
  (attrs: Modifier *)
  (validatingAttrs: (Var[Boolean] => Modifier) *)
  (implicit validator: Validator[String,T]) extends Field[T] {

  override val isValid: Var[Boolean] = Var(false)

  override lazy val field: dom.HTMLInputElement = scalatags.JsDom.tags.input(
    onkeyup := { () => isValid() = validator.validate(field.value) },
    attrs,
    validatingAttrs.map(_(isValid))
  ).render

  def validated: T = {
    validator.create(field.value)
  }

  def populate(inp: T): Unit = {
    field.value = validator.unbuild(inp)
    isValid() = true
  }

  def construct(): T = {
    validated
  }

  def apply[Foo](key: String) = this
}