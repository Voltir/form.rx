package formidable

import utest._
import scalatags.JsDom.all._
import formidable._
import formidable.Fields._
import formidable.validators.any._
import org.scalajs.dom


object BasicTests extends TestSuite {

  case class Thingy(foo: String, bar: String)
  case class Thingz(foo: String, renamed: String, another: String)
  case class Basic1(inp: String)

  def tests = TestSuite {
    'case2 {
      import FormidableTryAgain._

      val kill = Thingy("Foo","Bar")
      val wat = new Formidable(Mapper(
        "aaa"  -> new Basic[String](AnyString)(`type`:="text"),
        "bbb"  -> new Basic[String](AnyString)(`type`:="text")
      )(Thingy.apply)(Thingy.unapply))
      wat.populate(Thingy("QQQ","ZZZ"))
    }
    'nested {
      import FormidableTryAgain._
      case class Thingy(foo: String, bar: String)
      case class NestIt(baz: String, thingy: Thingy)

      val wat = new Formidable[NestIt](Mapper(
        "aaa"  -> new InputField,
        "bbb"  -> Mapper (
          "foo" -> new InputField,
          "bar" -> new InputField
        )(Thingy.apply)(Thingy.unapply)
      )(NestIt.apply)(NestIt.unapply))
    }

    'again {
      import FormidableThisTimeBetter._

      val foo = Thingz("YOLOYOLO","BITNNHZ","ANOTHER")

      object ThingyLayout {
        val foo = scalatags.JsDom.tags.input(`type`:="text").render
        val another = scalatags.JsDom.tags.input(`type`:="text").render
        val renamed = scalatags.JsDom.tags.input(`type`:="text").render
      }

      //val zzz = new ThingyLayout
      val wat: Formidable[Thingz] = MacroTest.fwat[ThingyLayout.type,Thingz](ThingyLayout)
      wat.populate(foo)
      //println(zzz.foo.value)
      //println(zzz.renamed.value)
      //println(zzz.another.value)
      println(ThingyLayout.another.value)
      ThingyLayout.foo.value = "DDDDDDDDDIIIIIIIIIIEEEEEEEEEE"
      val meh = wat.construct()
      println(meh)
      println(meh)
      println(meh)
    }
  }
}
