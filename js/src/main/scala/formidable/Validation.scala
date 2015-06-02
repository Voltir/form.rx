//package formidable
//
//import scala.util.{Try,Success,Failure}
//import scalatags.JsDom.all._
//import rx._
//import rx.ops._
//
//object Validation {
//
//  case object Unitialized extends Throwable("Unitialized Field")
//
//  class Validate[T]
//      (check: String => Try[T], asString: T => String, mods: Modifier*)
//      (rxMods: (Var[Try[T]] => Modifier)*) extends Formidable[T] {
//
//    val current: Var[Try[T]] = Var(Failure(Unitialized))
//
//    lazy val input: org.scalajs.dom.html.Input = scalatags.JsDom.all.input(
//      `type` := "text",
//      onkeyup := { () => current() = check(input.value)},
//      mods,
//      rxMods.map(_(current))
//    ).render
//
//    override def build(): Try[T] = current()
//
//    override def unbuild(inp: T) = {
//      current() = Success(inp)
//      input.value = asString(inp)
//    }
//
//  }
//
//  class ValidateNumber[N: Numeric, T]
//      (check: N => Try[T], asNumber: T => N, fromString: String => Try[N], mods: Modifier*)
//      (rxMods: (Var[Try[T]] => Modifier)*) extends Formidable[T] {
//
//    val current: Var[Try[T]] = Var(Failure(Unitialized))
//
//    lazy val input: org.scalajs.dom.html.Input = scalatags.JsDom.all.input(
//      `type` := "text",
//      onkeyup := { () => current() = fromString(input.value).flatMap(check) },
//      mods,
//      rxMods.map(_(current))
//    ).render
//
//    override def build(): Try[T] = current()
//
//    override def unbuild(inp: T) = {
//      current() = Success(inp)
//      input.value = asNumber(inp).toString
//    }
//  }
//
//  object Validate {
//    def apply[T](check: String => Try[T], asString: T => String, mods: Modifier*)(rxMods: (Var[Try[T]] => Modifier)*)  = new Validate(check,asString,mods:_*)(rxMods:_*)
//    def int[T](check: Int => Try[T], asInt: T => Int, mods: Modifier*)(rxMods: (Var[Try[T]] => Modifier)*) = new ValidateNumber[Int,T](check, asInt, q => Try(q.toInt) , mods:_*)(rxMods:_*)
//  }
//}