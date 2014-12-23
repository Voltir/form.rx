package formidable

import utest._
import scalatags.JsDom.all._
import formidable._
import formidable.Fields._
import formidable.validators.any._
import org.scalajs.dom

object BasicTests extends TestSuite {

  def tests = TestSuite {
    'case2 {
      import FormidableTryAgain._

      case class Thingy(foo: String, bar: String)

      val wat = new Formidable(Mapper(
        "aaa"  -> new Basic[String](AnyString)(`type`:="text"),
        "bbb"  -> new Basic[String](AnyString)(`type`:="text")
      )(Thingy.apply)(Thingy.unapply))

      wat.populate(Thingy("QQQ","ZZZ"))
      //assert(wat("aaa").asInstanceOf[dom.HTMLInputElement].value == "QQQ")
      //assert(wat("bbb").asInstanceOf[dom.HTMLInputElement].value == "ZZZ")
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
  }
}
