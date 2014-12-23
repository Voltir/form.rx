package formidable

import rx._

import scalatags.JsDom._

object Fields {
  class Basic[T](v: Validator[String,T])(attrs: Modifier *) extends InputField[T](attrs:_*)()(v) with FormidableTryAgain.Mapper[T]
}
