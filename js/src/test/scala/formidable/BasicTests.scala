package formidable

import utest._
import scalatags.JsDom.all._
import scala.util._

object BasicTests extends TestSuite {

  //object3 test
  case class Thing(foo: String, bar: String, baz: String)

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
      import formidable.Implicits._

      val foo = Thing("A","BB","CCC")

      trait ThingLayout {
        val foo = scalatags.JsDom.tags.input(`type`:="text").render
        val bar = scalatags.JsDom.tags.input(`type`:="text").render
        val baz = scalatags.JsDom.tags.input(`type`:="text").render
      }

      val test = Formidable[ThingLayout,Thing]
      test.unbuild(foo)
      assert(test.foo.value == "A")
      assert(test.bar.value == "BB")
      assert(test.baz.value == "CCC")
      test.foo.value = "MODIFIED"
      val created: Thing = test.build().get
      assert(created.foo == "MODIFIED")
      assert(created.bar == "BB")
      assert(created.baz == "CCC")
    }
//    'ignored {
//      import formidable.Implicits._
//      trait ThingIgnoreLayout {
//        val foo = Ignored("X")
//        val bar = Ignored("Y")
//        val baz = scalatags.JsDom.tags.input(`type`:="text").render
//      }
//      val foo = Thing("A","BB","CCC")
//      val test = Formidable[ThingIgnoreLayout,Thing]
//      test.unbuild(foo)
//      assert(test.baz.value == "CCC")
//      test.baz.value = "MODIFIED"
//      val created: Thing = test.build().get
//      assert(created.foo == "X")
//      assert(created.bar == "Y")
//      assert(created.baz == "MODIFIED")
//    }
//    'nested {
//      import formidable.Implicits._
//
//      trait InnerLayout {
//        val a = scalatags.JsDom.tags.input(`type`:="text").render
//        val b = scalatags.JsDom.tags.input(`type`:="text").render
//      }
//      trait OuterLayout {
//        val foo = scalatags.JsDom.tags.input(`type`:="text").render
//        val inner = Formidable[InnerLayout,Inner]
//      }
//      val test = Formidable[OuterLayout,Outer]
//      test.unbuild(Outer("LOL",Inner("QQQ","ZZZ")))
//      assert(test.inner.b.value == "ZZZ")
//      test.inner.a.value = "Modified"
//      test.inner.b.value = "Modified"
//      test.foo.value = "Modified"
//      val created = test.build().get
//      assert(created.foo == "Modified")
//      assert(created.inner.a == "Modified")
//      assert(created.inner.b == "Modified")
//    }
//    'notstrings {
//      import formidable.Implicits._
//      import scalatags.JsDom.tags.input
//      import org.scalajs.dom
//
//      implicit object WrappedBinder extends Binder[dom.HTMLInputElement,Wrapped] {
//        def bind(inp: dom.HTMLInputElement, value: Wrapped): Unit = { inp.value = value.value.toString }
//        def unbind(inp: dom.HTMLInputElement): Try[Wrapped] = { Success(Wrapped(inp.value.toLong)) }
//      }
//
//      trait NotStringLayout {
//        val foo = input(`type`:="text").render
//        val bar = input(`type`:="text").render
//        val baz = SelectionOf[ChoiceLike]()(
//          Opt(FirstChoice)(value:="first", "Yolo"),
//          Opt(SecondChoice)(value:="second", "Booof"),
//          Opt(ThirdChoice("Foo",42))(value:="third", "wizzyiiziz")
//        )
//      }
//
//      val test = Formidable[NotStringLayout,NotStrings]
//      test.unbuild(NotStrings(111,Wrapped(222),SecondChoice))
//      assert(test.foo.value == "111")
//      assert(test.bar.value == "222")
//      assert(test.baz.build == Success(SecondChoice))
//    }
  }
}
