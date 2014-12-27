package formidable

import utest._
import scalatags.JsDom.all._
import formidable._
import formidable.Fields._
import formidable.validators.any._
import org.scalajs.dom


object BasicTests extends TestSuite {

  case class Thingy(foo: String, bar: String)
  case class Thingz(foo: String, renamed: String, another: String, mostly: String)
  case class Basic1(inp: String)

  def tests = TestSuite {
    'object4 {
      import FormidableThisTimeBetter._

      val foo = Thingz("YOLOYOLO","BITNNHZ","ANOTHER","OMGOMOGMOGMOMGMOG")

      object ThingyLayout {
        val foo = scalatags.JsDom.tags.input(`type`:="text").render
        val another = scalatags.JsDom.tags.input(`type`:="text").render
        val renamed = scalatags.JsDom.tags.input(`type`:="text").render
        val mostly = scalatags.JsDom.tags.input(`type`:="text").render
      }
      val wat: Formidable[Thingz] = MacroTest.fwat[ThingyLayout.type,Thingz](ThingyLayout)
      wat.populate(foo)
      println(ThingyLayout.another.value)
      ThingyLayout.foo.value = "DDDDDDDDDIIIIIIIIIIEEEEEEEEEE"
      val meh = wat.construct()
      println(meh)
    }
  }
}
