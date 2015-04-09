package formidable.implicits

import formidable.Typeclasses.StringConstructable
import formidable.{BindRx, KCode, KeyboardPolyfill, FormidableRx}
import org.scalajs.dom
import org.scalajs.dom.html

import scala.collection.generic.CanBuildFrom
import scala.scalajs.js
import scala.util.{Failure, Success, Try}
import scalatags.JsDom.TypedTag
import scalatags.JsDom.all._
import collection.{ mutable => mut }
import KeyboardPolyfill._

trait Input {
  //Helper trait to shove Rx[Try[T]] into a dom.html.Input

  case object Unitialized extends Throwable("Unitialized Field")

  trait InputRxDynamic[T] {
    protected def bindDynamic(inp: html.Input)(make: String => Try[T]): rx.Rx[Try[T]] = {
      if(inp.asInstanceOf[js.Dynamic].selectDynamic("_inp_rx") == js.undefined) {
        val result: rx.Rx[Try[T]] = rx.Rx { make(inp.value) }
        inp.onkeyup = (ev:dom.KeyboardEvent) => { result.recalc() }
        inp.asInstanceOf[js.Dynamic].updateDynamic("_inp_rx")(result.asInstanceOf[js.Any])
        result
      } else {
        inp.asInstanceOf[js.Dynamic]._inp_rx.asInstanceOf[rx.Rx[Try[T]]]
      }
    }
  }

  //Binder for dom.html.Input
  class InputBindRx[Target: StringConstructable]
      extends BindRx[dom.html.Input,Target]
      with InputRxDynamic[Target] {
    val builder = implicitly[StringConstructable[Target]]

    def bind(inp: dom.html.Input, value: Target): Unit = {
      inp.value = builder.asString(value)
      bindDynamic(inp)(s => builder.parse(inp.value)).recalc()
    }

    def unbind(inp: dom.html.Input): rx.Rx[Try[Target]] = {
      bindDynamic(inp)(s => builder.parse(inp.value))
    }
  }

  implicit def inputBindRx[Target: StringConstructable]: BindRx[dom.html.Input,Target] = new InputBindRx[Target]

  class InputOptionBindRx[Target: StringConstructable]
    extends BindRx[dom.html.Input,Option[Target]]
    with InputRxDynamic[Option[Target]] {
    val builder = implicitly[StringConstructable[Target]]

    def bind(inp: dom.html.Input, value: Option[Target]): Unit = {
      val result = bindDynamic(inp)(s => Success(builder.parse(inp.value).toOption))
      inp.value = value.map(builder.asString).getOrElse("")
      result.recalc()
    }

    def unbind(inp: dom.html.Input): rx.Rx[Try[Option[Target]]] = {
      bindDynamic(inp)(s => Success(builder.parse(inp.value).toOption))
    }
  }

  implicit def inputOptionBindRx[Target: StringConstructable]: BindRx[dom.html.Input, Option[Target]] = new InputOptionBindRx[Target]

  implicit object IdentityStringConstrucatable extends StringConstructable[String] {
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

  //For List of Things (ie Tag Like)
  class TextRxBufferList[T, Layout <: FormidableRx[T]]
      (val inputTag: TypedTag[dom.html.Input])
      (val fromString: String => T)
      (val newLayout: T => Layout) extends FormidableRx[List[T]] {

    val values: rx.Var[mut.Buffer[Layout]] = rx.Var(mut.Buffer.empty)

    def pop() = values() = values.now - values().last

    lazy val current: rx.Rx[Try[List[T]]] = rx.Rx { Try {
      values().map(_.current().get).toList
    }}

    override def unbuild(newValues: List[T]) = {
      values.now.foreach { r => r.current.kill() }
      values() = newValues.map { t => newLayout(t)}.toBuffer
    }

    protected def handleKeyUp: js.ThisFunction1[dom.html.Input, dom.KeyboardEvent,Unit] = {
      (jsThis: dom.html.Input, evt: dom.KeyboardEvent) => {
        val key = evt.polyfill()._1

        def doUpdate(meh: Int) = {
          val elem = fromString(jsThis.value.take(jsThis.value.size - meh))
          val layout = newLayout(elem)
          jsThis.value = ""
          values.now.append(layout)
          values.recalc()
        }

        if(key == KCode.Comma) doUpdate(1)

        if(key == KCode.Enter) doUpdate(0)

        if(key == KCode.Backspace && jsThis.value == "" && values().size > 0) {
          pop()
        }
      }
    }

    lazy val input: dom.html.Input = inputTag(
      `type`:="text",
      scalatags.JsDom.all.onkeyup := handleKeyUp
    ).render
  }

  //For Set of Things (ie Tag Like)
  class TextRxSet[T, Layout <: FormidableRx[T]]
    (val inputTag: TypedTag[dom.html.Input])
    (val fromString: String => T)
    (val newLayout: T => Layout) extends FormidableRx[Set[T]] {

    val values: rx.Var[mut.Set[Layout]] = rx.Var(mut.Set.empty)

    def pop() = values() = values.now - values().last

    lazy val current: rx.Rx[Try[Set[T]]] = rx.Rx { Try {
      values().map(_.current().get).toSet
    }}

    override def unbuild(newValues: Set[T]) = {
      values.now.foreach { r => r.current.kill() }
      values() = mut.Set(newValues.map { t => newLayout(t)}.toSeq:_*)
    }

    protected def handleKeyUp: js.ThisFunction1[dom.html.Input, dom.KeyboardEvent,Unit] = {
      (jsThis: dom.html.Input, evt: dom.KeyboardEvent) => {

        val key = evt.polyfill()._1

        def doUpdate(meh: Int) = {
          val elem = fromString(jsThis.value.take(jsThis.value.size - meh))
          if(!current.now.toOption.exists(_.contains(elem))) {
            val layout = newLayout(elem)
            values.now += layout
            jsThis.value = ""
            values.recalc()
          }
        }

        if(key == KCode.Comma) doUpdate(1)

        if(key == KCode.Enter) doUpdate(0)

        if(key == KCode.Backspace && jsThis.value == "" && values().size > 0) {
          pop()
        }
      }
    }

    lazy val input: dom.html.Input = inputTag(
      `type`:="text",
      scalatags.JsDom.all.onkeyup := handleKeyUp
    ).render
  }


  class ValidateNext[T: StringConstructable](mods: Modifier*)(rxMods: (rx.Var[Try[T]] => Modifier)*) extends FormidableRx[T] {

    val _current: rx.Var[Try[T]] = rx.Var(Failure(Unitialized))

    private val builder = implicitly[StringConstructable[T]]

    val current: rx.Rx[Try[T]] = rx.Rx(_current())

    lazy val input: org.scalajs.dom.html.Input = scalatags.JsDom.all.input(
      `type` := "text",
      onkeyup := { () => _current() = builder.parse(input.value) },
      mods,
      rxMods.map(_(_current))
    ).render

    override def unbuild(inp: T) = {
      println("I WANT TO UNBUILD VALIDATE NEXT ----- " + inp)
      input.value = builder.asString(inp)
      _current() = Success(inp)
    }

  }
  object InputRx {
    //def autocomplete = ???
    def validate[T: StringConstructable](mods: Modifier *)= new ValidateNext[T](mods)()
    def  set[T, Layout <: FormidableRx[T]](inputTag: TypedTag[dom.html.Input])(fromString: String => T)(newLayout: T => Layout) = new TextRxSet[T,Layout](inputTag)(fromString)(newLayout)
    def list[T, Layout <: FormidableRx[T]](inputTag: TypedTag[dom.html.Input])(fromString: String => T)(newLayout: T => Layout) = new TextRxBufferList[T,Layout](inputTag)(fromString)(newLayout)
  }

}
