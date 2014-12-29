package formidable


object Implicits {
  import org.scalajs.dom.HTMLInputElement
  import scalatags.JsDom.all._

  trait Binder[I,O] {
    def bind(inp: I, value: O): Unit
    def unbind(inp: I): O
  }

  class LayoutBinder[L,T] extends Binder[L with Formidable[T],T]{
    def bind(inp: L with Formidable[T], value: T) = inp.populate(value)
    def unbind(inp: L with Formidable[T]): T = inp.construct()
  }
  implicit def ImplicitLayoutBinder[L,T]: Binder[L with Formidable[T],T] = new LayoutBinder()

  //Binder for Ignored fields
  class Ignored[T](val default: T)
  object Ignored {
    def apply[T](default: T) = new Ignored(default)
  }
  class IgnoredBinder[T] extends Binder[Ignored[T],T] {
    override def bind(inp: Ignored[T], value: T): Unit = Unit
    override def unbind(inp: Ignored[T]): T = inp.default
  }
  implicit def implicitIgnoreBinder[T]: Binder[Ignored[T],T] = new IgnoredBinder[T]

  //Binder for HTMLInputElement
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

  //Binders for T <=> Select element
  class Opt[+T](val value: T)(mods: Modifier *) {
    val option = scalatags.JsDom.all.option(mods)
  }

  object Opt {
    def apply[T](value: T)(mods: Modifier *) = new Opt(value)(mods)
  }

  class SelectionOf[T](options: Opt[T] *) {
    val select = scalatags.JsDom.all.select(options.map(_.option):_*).render

    def set(value: T) = options.zipWithIndex.find(_._1.value == value).foreach { case (opt,idx) =>
      select.selectedIndex = idx
    }

    def get = options(select.selectedIndex).value
  }

  object SelectionOf {
    def apply[T](options: Opt[T] *) = new SelectionOf[T](options:_*)
  }

  class SelectionOfBinder[T] extends Binder[SelectionOf[T],T] {
    def bind(inp: SelectionOf[T], value: T) = inp.set(value)
    def unbind(inp: SelectionOf[T]): T = inp.get
  }

  implicit def implicitSelectionOfBinder[T]: Binder[SelectionOf[T],T] = new SelectionOfBinder[T]

  //Binders for T <=> Radio elements
  //Binders for Set[T] <=> Checkbox elements
  class Chk[+T](val value: T)(mods: Modifier *) {
    val input = scalatags.JsDom.all.input(`type`:="checkbox",mods).render
  }

  object Chk {
    def apply[T](value: T)(mods: Modifier *) = new Chk(value)(mods)
  }

  class CheckboxSet[T](name: String)(checks: Chk[T] *) {
    val checkboxes: Array[Chk[T]] = {
      checks.map { c => c.input.name = name; c }.toArray
    }

    def set(values: Set[T]) = {
      val (checked,unchecked) = checks.partition(c => values.contains(c.value))
      checked.foreach   { _.input.checked = true  }
      unchecked.foreach { _.input.checked = false }
    }

    def get: Set[T] = {
      checks.filter(_.input.checked).map(_.value).toSet
    }
  }

  object CheckboxSet {
    def apply[T](name: String)(checks: Chk[T] *) = new CheckboxSet[T](name)(checks:_*)
  }

  class CheckboxSetBinder[T] extends Binder[CheckboxSet[T], Set[T]] {
    override def bind(inp: CheckboxSet[T], value: Set[T]): Unit = inp.set(value)
    override def unbind(inp: CheckboxSet[T]): Set[T] = inp.get
  }

  implicit def implicitCheckboxSetBinder[T]: Binder[CheckboxSet[T], Set[T]] = new CheckboxSetBinder[T]

  //Binders for Boolean <=> checkbox
  class CheckboxBool(mods: Modifier *) {
    val input = scalatags.JsDom.all.input(`type`:="checkbox", mods).render
  }
  object CheckboxBool {
    def apply(mods: Modifier *) = new CheckboxBool(mods:_*)
  }
  class CheckboxBoolBinder extends Binder[CheckboxBool,Boolean] {
    override def bind(inp: CheckboxBool, value: Boolean): Unit = inp.input.checked = value
    override def unbind(inp: CheckboxBool): Boolean = inp.input.checked
  }
  implicit def implicitCheckboxBoolBinder = new CheckboxBoolBinder
}
