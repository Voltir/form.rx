package formidable

import utest._
import scalatags.JsDom.all._
import formidable._
import formidable.Fields._
import formidable.validators.any._
import org.scalajs.dom


object todo {
  import org.scalajs.dom.HTMLInputElement

  trait Binder[I,O] {
    def bind(inp: I, value: O): Unit
    def unbind(inp: I): O
  }

  implicit object InputBinder extends Binder[HTMLInputElement,String] {
    def bind(inp: HTMLInputElement, value: String): Unit = { inp.value = value }
    def unbind(inp: HTMLInputElement): String = { inp.value }
  }

  class LayoutBinder[L,T] extends Binder[L with FormidableThisTimeBetter.Formidable[T],T]{
    def bind(inp: L with FormidableThisTimeBetter.Formidable[T], value: T) = {
      inp.populate(value)
    }
    def unbind(inp: L with FormidableThisTimeBetter.Formidable[T]): T = {
      inp.construct()
    }
  }

  implicit def ImplicitLayoutBinder[L,T]: Binder[L with FormidableThisTimeBetter.Formidable[T],T] = new LayoutBinder()
}

object BasicTests extends TestSuite {

  case class Thing(foo: String, bar: String, baz: String)

  case class Inner(a: String, b: String)

  case class Outer(foo: String, inner: Inner)

  def tests = TestSuite {
    'object3 {
      import FormidableThisTimeBetter._
      import formidable.todo._

      val foo = Thing("A","BB","CCC")

      object ThingLayout {
        val foo = scalatags.JsDom.tags.input(`type`:="text").render
        val bar = scalatags.JsDom.tags.input(`type`:="text").render
        val baz = scalatags.JsDom.tags.input(`type`:="text").render
      }

      val wat: Formidable[Thing] = MacroTest.fwat[ThingLayout.type,Thing](ThingLayout)
      wat.populate(foo)
      assert(ThingLayout.foo.value == "A")
      assert(ThingLayout.bar.value == "BB")
      assert(ThingLayout.baz.value == "CCC")
      ThingLayout.foo.value = "MODIFIED"
      val meh = wat.construct()
      assert(meh.foo == "MODIFIED")
      assert(meh.bar == "BB")
      assert(meh.baz == "CCC")
      println(meh)
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
      println(test.inner.b.value)
    }
  }
}
