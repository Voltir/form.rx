package formidable.implicits

import formidable.Typeclasses.StringConstructable
import formidable.{BindRx, KCode, KeyboardPolyfill, FormidableRx}
import org.scalajs.dom
import org.scalajs.dom.html
import rx._
import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.util.{Failure, Success, Try}
import scalatags.JsDom.TypedTag
import scalatags.JsDom.all._
import collection.{ mutable => mut }
import KeyboardPolyfill._

trait Input {
  //Helper trait to shove Rx[Try[T]] into a dom.html.Input
  trait InputRxDynamic[T] {
    private val KEY = "_inp_rx"
    protected def bindDynamic(inp: html.Input)(make: String => Try[T]): Var[Try[T]] = {
      val bound = inp.asInstanceOf[js.Dynamic].selectDynamic(KEY).asInstanceOf[UndefOr[Var[Try[T]]]]
      bound.getOrElse {
        val result: Var[Try[T]] = Var(make(inp.value))
        inp.onkeyup = (ev:dom.KeyboardEvent) => result() = make(inp.value)
        inp.asInstanceOf[js.Dynamic].updateDynamic(KEY)(result.asInstanceOf[js.Any])
        result
      }
    }
  }

  //Binder for dom.html.Input
  class InputBindRx[Target: StringConstructable]
      extends BindRx[dom.html.Input,Target]
      with InputRxDynamic[Target] {
    
    private val builder = implicitly[StringConstructable[Target]]
    
    private val make = (s: String) => builder.parse(s)

    private def update(inp: dom.html.Input): Unit = {
      val dynamicVar = bindDynamic(inp)(make)
      dynamicVar() = make(inp.value)
    }

    override def bind(inp: dom.html.Input, value: Target): Unit = {
      inp.value = builder.asString(value)
      update(inp)
    }

    override def unbind(inp: dom.html.Input): rx.Node[Try[Target]] = {
      bindDynamic(inp)(make)
    }

    override def reset(inp: dom.html.Input): Unit = {
      inp.value = ""
      update(inp)
    }
  }

  implicit def inputBindRx[Target: StringConstructable]: BindRx[dom.html.Input,Target] = new InputBindRx[Target]

  //Basic String Constructable Implicits
  implicit object StringStringConstructable extends StringConstructable[String] {
    def asString(inp: String): String = inp
    def parse(txt: String): Try[String] = Success(txt)
  }

  implicit object IntStringConstructable extends StringConstructable[Int] {
    def asString(inp: Int): String = inp.toString
    def parse(txt: String): Try[Int] = Try(txt.toInt)
  }

  implicit object FloatStringConstructable extends StringConstructable[Float] {
    def asString(inp: Float): String = inp.toString
    def parse(txt: String): Try[Float] = Try(txt.toFloat)
  }

  implicit object LongStringConstructable extends StringConstructable[Long] {
    def asString(inp: Long): String = inp.toString
    def parse(txt: String): Try[Long] = Try(txt.toLong)
  }

  implicit object DoubleStringConstructable extends StringConstructable[Double] {
    def asString(inp: Double): String = inp.toString
    def parse(txt: String): Try[Double] = Try(txt.toDouble)
  }

  class OptStringConstructable[T: StringConstructable] extends StringConstructable[Option[T]] {
    val builder = implicitly[StringConstructable[T]]
    def asString(inp: Option[T]) = inp.map(builder.asString).getOrElse("")
    def parse(txt: String): Try[Option[T]] = scala.util.Success {
      if(txt.length == 0) None else builder.parse(txt).toOption
    }
  }

  implicit def OptionStringConstructable[T: StringConstructable]: StringConstructable[Option[T]] = {
    new OptStringConstructable[T]
  }

  //For List of Things (ie Tag Like)
  class TextRxBufferList[T, Layout <: FormidableRx[T]]
      (val inputTag: TypedTag[dom.html.Input])
      (val fromString: String => T)
      (val newLayout: () => Layout) extends FormidableRx[List[T]] {

    val values: rx.Var[mut.Buffer[Layout]] = rx.Var(mut.Buffer.empty)

    def pop() = values() = values.now - values().last

    lazy val current: rx.Rx[Try[List[T]]] = rx.Rx { Try {
      values().map(_.current().get).toList
    }}

    override def set(newValues: List[T]) = {
      values.now.foreach { r => r.current.kill() }
      values() = newValues.map { t =>
        val layout = newLayout()
        layout.set(t)
        layout
      }.toBuffer
    }

    override def reset(): Unit = {
      set(List.empty)
    }

    protected def handleKeyInput: js.ThisFunction1[dom.html.Input, dom.KeyboardEvent,Unit] = {
      (jsThis: dom.html.Input, evt: dom.KeyboardEvent) => {
        val key = evt.polyfill()._1

        def doUpdate = {
          evt.stopPropagation()
          evt.preventDefault()
          val elem = fromString(jsThis.value)
          val layout = newLayout()
          layout.set(elem)
          jsThis.value = ""
          values.now.append(layout)
          values.propagate()
        }

        if(key == KCode.Comma) doUpdate

        if(key == KCode.Enter) doUpdate

        if(key == KCode.Backspace && jsThis.value == "" && values().nonEmpty) {
          pop()
        }
      }
    }

    lazy val input: dom.html.Input = inputTag(
      `type`:="text",
      scalatags.JsDom.all.onkeydown := handleKeyInput
    ).render
  }

  //For Set of Things (ie Tag Like)
  class TextRxSet[T, Layout <: FormidableRx[T]]
    (val inputTag: TypedTag[dom.html.Input])
    (val fromString: String => T)
    (val newLayout: () => Layout) extends FormidableRx[Set[T]] {

    val values: rx.Var[Set[Layout]] = rx.Var(Set.empty)

    def pop() = values() = values.now - values().last

    val current: rx.Rx[Try[Set[T]]] = rx.Rx { Try {
      values().flatMap(_.current().toOption)
    }}

    override def set(newValues: Set[T]) = {
      values.now.foreach { r => r.current.kill() }
      values() = newValues.map { t => val r = newLayout() ; r.set(t) ; r }
    }

    override def reset(): Unit = set(Set.empty)

    protected def handleKeyInput: js.ThisFunction1[dom.html.Input, dom.KeyboardEvent,Unit] = {
      (jsThis: dom.html.Input, evt: dom.KeyboardEvent) => {

        val key = evt.polyfill()._1

        def doUpdate(): Unit = {
          evt.stopPropagation()
          evt.preventDefault()
          val elem = fromString(jsThis.value)
          if(!current.now.toOption.exists(_.contains(elem))) {
            val layout = newLayout()
            layout.set(elem)
            jsThis.value = ""
            values() = values.now + layout
          }
        }

        if(key == KCode.Comma) doUpdate()

        if(key == KCode.Enter) doUpdate()

        if(key == KCode.Backspace && jsThis.value == "" && values().nonEmpty) {
          pop()
        }
      }
    }

    lazy val input: dom.html.Input = inputTag(
      `type`:="text",
      scalatags.JsDom.all.onkeydown := handleKeyInput
    ).render
  }

  class Validate[T: StringConstructable](defaultToUninitialized: Boolean)(mods: Modifier*) extends FormidableRx[T] {

    private lazy val defaultValue = if(defaultToUninitialized) Failure(formidable.FormidableUninitialized) else builder.parse("")

    private val _current: rx.Var[Try[T]] = rx.Var(defaultValue)

    private val builder = implicitly[StringConstructable[T]]

    val current: rx.Rx[Try[T]] = rx.Rx(_current())

    lazy val input: org.scalajs.dom.html.Input = scalatags.JsDom.all.input(
      `type` := "text",
      onkeyup := { () => _current() = builder.parse(input.value) },
      mods
    ).render

    override def set(inp: T) = {
      input.value = builder.asString(inp)
      _current() = Success(inp)
    }

    override def reset(): Unit = {
      input.value = ""
      _current() = defaultValue
    }
  }

  class ValidateMaybe[T: StringConstructable](mods: Modifier*) extends FormidableRx[Option[T]] {

    private val wrapped = new Validate[T](true)(mods)

    override val current: rx.Rx[Try[Option[T]]] = Rx {
      wrapped.current()
      if(input.value == "") Success(None)
      else wrapped.current().map(Option.apply)
    }

    lazy val input: org.scalajs.dom.html.Input = wrapped.input

    override def set(inp: Option[T]) = {
      if(inp.isEmpty) wrapped.reset()
      else wrapped.set(inp.get)
    }

    override def reset(): Unit = wrapped.reset()
  }


  object InputRx {
    //def autocomplete = ???
    def validate[T: StringConstructable](defaultToUninitialized: Boolean)(mods: Modifier *) = new Validate[T](defaultToUninitialized)(mods)
    def validateMaybe[T: StringConstructable](mods: Modifier *) = new ValidateMaybe[T](mods)
    def set[T, Layout <: FormidableRx[T]](inputTag: TypedTag[dom.html.Input])(fromString: String => T)(newLayout: () => Layout) = new TextRxSet[T,Layout](inputTag)(fromString)(newLayout)
    def list[T, Layout <: FormidableRx[T]](inputTag: TypedTag[dom.html.Input])(fromString: String => T)(newLayout: () => Layout) = new TextRxBufferList[T,Layout](inputTag)(fromString)(newLayout)
  }

}
