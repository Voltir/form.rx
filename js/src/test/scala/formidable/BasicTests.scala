package formidable

import org.scalajs.dom
import org.scalajs.dom.raw.KeyboardEvent
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
  import implicits.all._

  val foo = Stuff("A",42)

  trait StuffLayout {
    val foo = Var("aBarTxt")
    val bar = Var(999)
  }

  def tests = TestSuite {
    'object3 {

      val foo = Thing("A","BB","CCC")

      trait ThingLayout {
        val foo = scalatags.JsDom.tags.input(`type`:="text").render
        val bar = scalatags.JsDom.tags.input(`type`:="text").render
        val baz = scalatags.JsDom.tags.input(`type`:="text").render
      }

      val test = FormidableRx[ThingLayout,Thing]

      test.set(foo)
      //println("###### DONNNE ####")
      assert(test.foo.value == "A")
      assert(test.bar.value == "BB")
      assert(test.baz.value == "CCC")

      test.reset()
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
      assert(test.inner.current.now.get.a == "QQQ")
      assert(test.inner.current.now.get.b == "ZZZ")
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
        val bar = Var(Wrapped(42))
        val baz = SelectionRx[ChoiceLike]()(
          Opt(FirstChoice)(value:="first", "Yolo"),
          Opt(SecondChoice)(value:="second", "Booof"),
          Opt(ThirdChoice("Foo",42))(value:="third", "wizzyiiziz")
        )
      }

      val test = FormidableRx[NotStringLayout,NotStrings]
      test.bar() = Wrapped(82828282)
      test.reset()
    }

    'vars {
      val test = FormidableRx[StuffLayout,Stuff]

      test.set(foo)
      assert(test.foo.now == "A")
      assert(test.bar.now == 42)
      test.bar() = 9000
      test.foo() = "omgdifferent"
      val newStuff = test.current.now.get
      assert(newStuff.bar == 9000)
      assert(newStuff.foo == "omgdifferent")
      test.reset()
      assert(test.foo.now == "aBarTxt")
      assert(test.bar.now == 999)
    }

    'innerRxDependencies {
      val test = FormidableRx[StuffLayout,Stuff]
      val barRx = Rx { test.bar() + 2 }
      assert(barRx.now == 1001)
      test.set(foo)
      assert(barRx.now == 44)
      test.reset()
      assert(barRx.now == 1001)
    }
  }
}

//todo test CheckboxBaseRx

object CheckboxTests extends TestSuite {
  import implicits.all._

  case class Fruit(name: String)
  case class FruitBasket(fruits: Set[Fruit])

  val fruits = Set(
    Fruit("apple"),
    Fruit("banana"),
    Fruit("pear")
  )

  val fruitOpts: Var[List[Chk[Fruit]]] = Var(List.empty)

  trait FruitBasketLayout {
    val fruits = CheckboxRx.dynamicSet[Fruit]("fruits")(fruitOpts)
  }

  def tests = TestSuite {
    'dynamic {
      val form = FormidableRx[FruitBasketLayout, FruitBasket]

      // sanity
      val currentChecks = fruitOpts.now
      assert(currentChecks.isEmpty)

      // sanity
      val emptyBasket = form.current.now.get
      assert(emptyBasket.fruits.isEmpty)

      // update fruit options
      fruitOpts() = fruits.map(f => Chk(f)()).toList
      assert(fruitOpts.now.size == 3)

      // ensure checkbox names match
      val namesMatch = fruitOpts().forall(_.input.name == "fruits")
      assert(namesMatch)

      // set fruit form
      form.fruits.set(fruits.tail)
      assert(form.current.now.get.fruits.size == 2)

      // reset all options
      form.fruits.reset()
      assert(form.current.now.get.fruits.size == 0)
    }
  }
}