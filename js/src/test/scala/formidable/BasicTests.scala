package formidable

import formidable.Implicits.Opt
import utest._
import scalatags.JsDom.all._
import scala.util._
import rx._

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

//Testing Var resets
case class Stuff(foo: String, bar: Int)

object BasicTests extends TestSuite {

  def tests = TestSuite {
    'object3 {
      import implicits.all._
      val foo = Thing("A","BB","CCC")

      trait ThingLayout {
        val foo = scalatags.JsDom.tags.input(`type`:="text").render
        val bar = scalatags.JsDom.tags.input(`type`:="text").render
        val baz = scalatags.JsDom.tags.input(`type`:="text").render
      }

      val test = FormidableRx[ThingLayout,Thing]

      var before = 0
      var after = 0
      var rxCount = 0
      val obs = Rx {
        println("OMG TEST CHANGED")
        println(test.current())
        rxCount += 1
      }

      println("####################### SET FOO #################")
      before = rxCount ; test.set(foo) ; after = rxCount
      assert(after == before + 1)
      println("###### DONNNE ####")
      assert(test.foo.value == "A")
      assert(test.bar.value == "BB")
      assert(test.baz.value == "CCC")

      println("####################### RESET #################")
      before = rxCount ; test.reset() ; after = rxCount
      //todo make work: assert(after == before + 1)
      assert(test.foo.value == "")
      assert(test.bar.value == "")
      assert(test.baz.value == "")

    }
    'ignored {
      import implicits.all._
      trait ThingIgnoreLayout {
        val foo = Ignored("X")
        val bar = Ignored("Y")
        val baz = scalatags.JsDom.tags.input(`type`:="text").render
      }
      val foo = Thing("A","BB","CCC")
      val test = FormidableRx[ThingIgnoreLayout,Thing]
      test.set(foo)
      assert(test.baz.value == "CCC")
      assert(test.foo.current.now.get == "X")
      assert(test.bar.current.now.get == "Y")
      test.reset()
      assert(test.foo.current.now.get == "X")
      assert(test.bar.current.now.get == "Y")
      assert(test.baz.value == "")
    }
    'nested {
      import implicits.all._

      trait InnerLayout {
        val a = scalatags.JsDom.tags.input(`type`:="text").render
        val b = scalatags.JsDom.tags.input(`type`:="text").render
      }
      trait OuterLayout {
        val foo = scalatags.JsDom.tags.input(`type`:="text").render
        val inner = FormidableRx[InnerLayout,Inner]
      }
      val test = FormidableRx[OuterLayout,Outer]
      test.set(Outer("LOL",Inner("QQQ","ZZZ")))
      assert(test.inner.a.value == "QQQ")
      assert(test.inner.b.value == "ZZZ")
      assert(test.foo.value == "LOL")
      test.inner.reset()
      assert(test.inner.a.value == "")
      assert(test.inner.b.value == "")
      assert(test.foo.value == "LOL")
      test.set(Outer("LOL",Inner("QQQ","ZZZ")))
      test.reset()
      assert(test.inner.a.value == "")
      assert(test.inner.b.value == "")
      assert(test.foo.value == "")
    }
    'notstrings {
      import implicits.all._
      import scalatags.JsDom.tags.input
      import org.scalajs.dom

      trait NotStringLayout {
        val foo = input(`type`:="text").render
        val bar = Var(Wrapped(42))//VarRx(Wrapped(42))
        val baz = SelectionRx[ChoiceLike]()(
          Opt(FirstChoice)(value:="first", "Yolo"),
          Opt(SecondChoice)(value:="second", "Booof"),
          Opt(ThirdChoice("Foo",42))(value:="third", "wizzyiiziz")
        )
      }

      val test = FormidableRx[NotStringLayout,NotStrings]
      test.bar() = Wrapped(82828282)
      test.reset()
      //test.current(NotStrings(111,Wrapped(222),SecondChoice))
      //assert(test.foo.value == "111")
      //assert(test.bar.value == "222")
      //assert(test.baz.build == Success(SecondChoice))
    }
    'varResets {
      import implicits.all._
      val foo = Stuff("A",42)

      trait StuffLayout {
        val foo = Var("aBarTxt")
        val bar = Var(999)
      }

      val test = FormidableRx[StuffLayout,Stuff]
      test.set(foo)
      println(test)
      test.bar() = 42
      test.foo() = "DEATHKILLMURDER"
      println(test.current.now)
      println("OKOKOKKO")
      test.reset()
      println(test.current.now)
      assert(test.foo.now == "aBarTxt")
      assert(test.bar.now == 999)
    }
  }
}
