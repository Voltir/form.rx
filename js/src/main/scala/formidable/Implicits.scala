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


  //Binders for html select element
  class Opt[+T](val value: T)(mods: Modifier *) {
    val option = scalatags.JsDom.all.option(mods)
  }

  object Opt {
    def apply[T](value: T)(mods: Modifier *) = new Opt(value)(mods)
  }

  class SelectWith[T](options: Opt[T] *) {
    val select = scalatags.JsDom.all.select(options.map(_.option):_*).render

    def set(value: T) = options.zipWithIndex.find(_._1.value == value).foreach { case (opt,idx) =>
      select.selectedIndex = idx
    }

    def get = options(select.selectedIndex).value
  }

  class SelectWithBinder[T] extends Binder[SelectWith[T],T] {
    def bind(inp: SelectWith[T], value: T) = inp.set(value)
    def unbind(inp: SelectWith[T]): T = inp.get
  }

  implicit def SelectWithBinder[T]: Binder[SelectWith[T],T] = new SelectWithBinder[T]
}
