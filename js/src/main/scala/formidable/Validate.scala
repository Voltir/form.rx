package formidable

import org.scalajs.dom
import rx._
import rx.ops._
import scalatags.JsDom.all._

//WIP

trait Validator[V,T] extends Formidable[T] {
  def validate(inp: V): Boolean
}

class InputOf[T]
  (attrs: Modifier *)
  (validatingAttrs: (Var[Boolean] => Modifier) *)
  (implicit validator: Validator[String,T]) {

  val isValid: Var[Boolean] = Var(false)

  lazy val input: dom.HTMLInputElement = scalatags.JsDom.tags.input(
    onkeyup := { () => isValid() = validator.validate(input.value) },
    attrs,
    validatingAttrs.map(_(isValid))
  ).render

//  def validated: T = {
//    validator.create(field.value)
//  }
//
//  def populate(inp: T): Unit = {
//    field.value = validator.unbuild(inp)
//    isValid() = true
//  }
//
//  def construct(): T = {
//    validated
//  }

}