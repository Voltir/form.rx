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
}

object BasicTests extends TestSuite {

  case class Thing(foo: String, bar: String, baz: String)
  case class Basic1(inp: String)


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
  }
}
