package formidable

import utest._
import scalatags.JsDom.all._
import formidable._
import formidable.Fields._
import formidable.validators.any._
import org.scalajs.dom


object todo {
  import org.scalajs.dom.HTMLInputElement
  import scala.language.higherKinds

  trait Binder[I,O] {
    def bind(inp: I, value: O): Unit
    def unbind(inp: I): O
  }

  class LayoutBinder[L,T] extends Binder[L with FormidableThisTimeBetter.Formidable[T],T]{
    def bind(inp: L with FormidableThisTimeBetter.Formidable[T], value: T) = inp.populate(value)
    def unbind(inp: L with FormidableThisTimeBetter.Formidable[T]): T = inp.construct()
  }
  implicit def ImplicitLayoutBinder[L,T]: Binder[L with FormidableThisTimeBetter.Formidable[T],T] = new LayoutBinder()

  implicit object InputBinder extends Binder[HTMLInputElement,String] {
    def bind(inp: HTMLInputElement, value: String): Unit = { inp.value = value }
    def unbind(inp: HTMLInputElement): String = { inp.value }
  }

  class InputNumericBinder[N: Numeric](unbindf: String => N) extends Binder[HTMLInputElement,N] {
    def bind(inp: HTMLInputElement, value: N): Unit = { inp.value = value.toString }
    def unbind(inp: HTMLInputElement): N = { unbindf(inp.value) }
  }

  implicit val InputIntBinder = new InputNumericBinder[Int](_.toInt)
  implicit val InputLongBinder = new InputNumericBinder[Long](_.toLong)
  implicit val InputFloatBinder = new InputNumericBinder[Float](_.toFloat)
  implicit val InputDoubleBinder = new InputNumericBinder[Double](_.toDouble)


  //Binders for html select element
  class Opt[+T](val value: T)(mods: Modifier *) {
    val option = scalatags.JsDom.all.option(mods)
  }

  object Opt {
    def apply[T](value: T)(mods: Modifier *) = new Opt(value)(mods)
  }

  class SelectWith[T](options: Opt[T] *) {
    val select = scalatags.JsDom.all.select(options.map(_.option):_*).render

    def set(value: T) = options.zipWithIndex.find(_._1.value == value).foreach { case (opt,idx) =>
      select.selectedIndex = idx
    }

    def get = options(select.selectedIndex).value
  }

  class SelectWithBinder[T] extends Binder[SelectWith[T],T] {
    def bind(inp: SelectWith[T], value: T) = inp.set(value)
    def unbind(inp: SelectWith[T]): T = inp.get
  }

  implicit def SelectWithBinder[T]: Binder[SelectWith[T],T] = new SelectWithBinder[T]
}

object BasicTests extends TestSuite {

  //object3 test
  case class Thing(foo: String, bar: String, fwoop: String)

  //nested test
  case class Inner(a: String, b: String)
  case class Outer(foo: String, inner: Inner)

  //not strings test
  case class Wrapped(value: Long) extends AnyVal
  sealed trait ChoiceLike
  case object FirstChoice extends ChoiceLike
  case object SecondChoice extends ChoiceLike
  case class ThirdChoice(a: String, b: Int) extends ChoiceLike
  case class NotStrings(foo: Int, bar: Wrapped, baz: ChoiceLike)

  def tests = TestSuite {
    'object3 {
      import FormidableThisTimeBetter._
      import formidable.todo._

      val foo = Thing("A","BB","CCC")

      trait ThingLayout {
        val foo = scalatags.JsDom.tags.input(`type`:="text").render
        val bar = scalatags.JsDom.tags.input(`type`:="text").render
        val fwoop = scalatags.JsDom.tags.input(`type`:="text").render
      }

      val test = MacroTest.v2[ThingLayout,Thing]
      test.populate(foo)
      assert(test.foo.value == "A")
      assert(test.bar.value == "BB")
      assert(test.fwoop.value == "CCC")
      test.foo.value = "MODIFIED"
      val created: Thing = test.construct()
      assert(created.foo == "MODIFIED")
      assert(created.bar == "BB")
      assert(created.fwoop == "CCC")
    }
    'nested {
      import FormidableThisTimeBetter._
      import formidable.todo._

      trait InnerLayout {
        val a = scalatags.JsDom.tags.input(`type`:="text").render
        val b = scalatags.JsDom.tags.input(`type`:="text").render
      }
      trait OuterLayout {
        val foo = scalatags.JsDom.tags.input(`type`:="text").render
        val inner = MacroTest.v2[InnerLayout,Inner]
      }
      val test = MacroTest.v2[OuterLayout,Outer]
      test.populate(Outer("LOL",Inner("QQQ","ZZZ")))
      assert(test.inner.b.value == "ZZZ")
      test.inner.a.value = "Modified"
      test.inner.b.value = "Modified"
      test.foo.value = "Modified"
      val created = test.construct()
      assert(created.foo == "Modified")
      assert(created.inner.a == "Modified")
      assert(created.inner.b == "Modified")
    }
    'notstrings {
      import FormidableThisTimeBetter._
      import formidable.todo._
      import scalatags.JsDom.tags.input
      import org.scalajs.dom

      implicit object WrappedBinder extends Binder[dom.HTMLInputElement,Wrapped] {
        def bind(inp: dom.HTMLInputElement, value: Wrapped): Unit = { inp.value = value.value.toString }
        def unbind(inp: dom.HTMLInputElement): Wrapped = { Wrapped(inp.value.toLong) }
      }

      trait NotStringLayout {
        val foo = input(`type`:="text").render
        val bar = input(`type`:="text").render
        val baz = new SelectWith[ChoiceLike](
          Opt(FirstChoice)(value:="first", "Yolo"),
          Opt(SecondChoice)(value:="second", "Booof"),
          Opt(ThirdChoice("Foo",42))(value:="third", "wizzyiiziz")
        )
      }

      val test = MacroTest.v2[NotStringLayout,NotStrings]
      test.populate(NotStrings(111,Wrapped(222),SecondChoice))
      assert(test.foo.value == "111")
      assert(test.bar.value == "222")
      assert(test.baz.get == SecondChoice)
    }
  }
}
