package formrx

import utest._
import scala.scalajs.js
import scalatags.JsDom.all._
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

object Testz {
  import implicits.all._
  trait Other { def other: Int = 42 }
  class ThingLayout()(implicit ctx: Ctx.Owner) extends Other {
    val foo = Var("")
    val bar = Var("")
    val baz = Var("")
  }

  val formz = FormRx[Thing,ThingLayout]
}

object BasicTests extends TestSuite {
  import implicits.all._

  implicit val testctx = Ctx.Owner.safe()

  val foo = Stuff("A",42)

  class StuffLayout(implicit ctx: Ctx.Owner) {
    val foo = Var("aBarTxt")
    val bar = Var(999)
  }

  def tests = TestSuite {
    'object3 {
      val foo = Thing("A","BB","CCC")

      class ThingLayout(implicit ctzzx: Ctx.Owner) {
        val foo = scalatags.JsDom.tags.input(`type`:="text").render
        val bar = scalatags.JsDom.tags.input(`type`:="text").render
        val baz = scalatags.JsDom.tags.input(`type`:="text").render
      }

      val test = FormRx[Thing,ThingLayout]

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
      abstract class ThingIgnoreLayout()(implicit ctx: Ctx.Owner) {
        val foo = Ignored("X")
        val bar = Ignored("Y")
        val baz = scalatags.JsDom.tags.input(`type`:="text").render
      }
      val foo = Thing("A","BB","CCC")
      val test = FormRx[Thing,ThingIgnoreLayout]
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

      class InnerLayout(implicit ctx: Ctx.Owner) {
        val a = scalatags.JsDom.tags.input(`type`:="text").render
        val b = scalatags.JsDom.tags.input(`type`:="text").render
      }
      class OuterLayout(implicit ctx: Ctx.Owner) {
        val foo = scalatags.JsDom.tags.input(`type`:="text").render
        val inner = FormRx[Inner,InnerLayout]
      }
      val test = FormRx[Outer,OuterLayout]
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

      class NotStringLayout(implicit ctx: Ctx.Owner) {
        val foo = input(`type`:="text").render
        val bar = Var(Wrapped(42))
        val baz = SelectionRx[ChoiceLike]()(
          Opt(FirstChoice)(value:="first", "Yolo"),
          Opt(SecondChoice)(value:="second", "Booof"),
          Opt(ThirdChoice("Foo",42))(value:="third", "wizzyiiziz")
        )
      }

      val test = FormRx[NotStrings,NotStringLayout]
      test.bar() = Wrapped(82828282)
      test.reset()
    }

    'vars {
      val test = FormRx[Stuff,StuffLayout]

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
      val test = FormRx[Stuff,StuffLayout]
      val barRx = Rx { test.bar() + 2 }
      assert(barRx.now == 1001)
      test.set(foo)
      assert(barRx.now == 44)
      test.reset()
      assert(barRx.now == 1001)
    }
  }
}

object CheckboxTests extends TestSuite {
  import implicits.all._

  implicit val testctx = Ctx.Owner.safe()

  case class Fruit(name: String)
  case class FruitBasket(fruits: Set[Fruit])
  case class FruitList(fruits: List[Fruit])

  val allFruits = Set(
    Fruit("apple"),
    Fruit("banana"),
    Fruit("pear")
  )

  val fruitOpts: Var[List[Chk[Fruit]]] = Var(List.empty)

  class FruitListLayout()(implicit ctx: Ctx.Owner) {
    val fruits = CheckboxRx.list[Fruit]("fruits")(allFruits.toList.map(f => Chk(f)()):_*)
  }

  class FruitBasketLayout1()(implicit ctx: Ctx.Owner) {
    val fruits = CheckboxRx.set[Fruit]("fruits")(allFruits.toList.map(f => Chk(f)()):_*)
  }

  class FruitBasketLayout2()(implicit ctx: Ctx.Owner) {
    val fruits = CheckboxRx.dynamicSet[Fruit]("fruits")(fruitOpts)
  }

  def tests = TestSuite {
    'list {
      val form = FormRx[FruitList,FruitListLayout]

      // sanity
      assert(form.current.now.get.fruits.isEmpty)
      assert(form.fruits.checkboxes.size == 3)

      form.fruits.checkboxes.foreach(_.input.checked = true)
      form.fruits.current.recalc()
      assert(form.current.now.get.fruits.size == 3)
    }

    'set {
      val form = FormRx[FruitBasket,FruitBasketLayout1]

      // sanity
      assert(form.current.now.get.fruits.isEmpty)
      assert(form.fruits.checkboxes.size == 3)

      form.fruits.checkboxes.foreach(_.input.checked = true)
      form.fruits.current.recalc()
      assert(form.current.now.get.fruits.size == 3)
    }

    'dynamicSet {
      val form = FormRx[FruitBasket,FruitBasketLayout2]

      // sanity
      val currentChecks = fruitOpts.now
      assert(currentChecks.isEmpty)

      // sanity
      val emptyBasket = form.current.now.get
      assert(emptyBasket.fruits.isEmpty)

      // update fruit options
      fruitOpts() = allFruits.map(f => Chk(f)()).toList
      assert(fruitOpts.now.size == 3)

      // ensure checkbox names match
      val namesMatch = fruitOpts.now.forall(_.input.name == "fruits")
      assert(namesMatch)

      // set fruit form
      form.fruits.set(allFruits.tail)
      assert(form.current.now.get.fruits.size == 2)

      // reset all options
      form.fruits.reset()
      assert(form.current.now.get.fruits.isEmpty)
    }
  }
}

object RadioTests extends TestSuite {
  import implicits.all._

  case class SomeChoice(choice: ChoiceLike)

  class SomeChoiceLayout()(implicit ctx: Ctx.Owner) {
    val choice = RadioRx[ChoiceLike]("choices")(
      Radio(FirstChoice)(),
      Radio(SecondChoice)(),
      Radio(ThirdChoice("foo", 42))()
    )
  }

  val form = FormRx[SomeChoice,SomeChoiceLayout]

  def tests = TestSuite {
    'radio {

      assert(form.choice.current.now.get == FirstChoice)

      form.set(SomeChoice(ThirdChoice("foo",42)))
      assert(form.choice.current.now.get == ThirdChoice("foo",42))
    }
  }
}