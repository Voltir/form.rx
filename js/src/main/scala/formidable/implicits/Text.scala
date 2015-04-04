package formidable.implicits

import formidable.ImplicitsNext.InputRxDynamic
import formidable.{BindRx, KCode, KeyboardPolyfill, FormidableRx}
import org.scalajs.dom

import scala.collection.generic.CanBuildFrom
import scala.scalajs.js
import scala.util.{Success, Try}
import scalatags.JsDom.TypedTag
import scalatags.JsDom.all._
import collection.{ mutable => mut }
import KeyboardPolyfill._

trait Text {

  //For Optional Things
  class OptionTextRx[T](inputTag: TypedTag[dom.html.Input])(make: String => Option[T], unmake: T => String) extends FormidableRx[Option[T]]  with InputRxDynamic[Option[T]] {

    lazy val input: dom.html.Input = inputTag.render

    override val current = bindDynamic(inputTag.render)(s => if(s == "") Try(Option.empty[T]) else Try(make(input.value)))

    override def unbuild(inp: Option[T]) = {
      input.value = inp.map(unmake).getOrElse("")
      current.recalc()
    }
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

        def doUpdate() = {
          val elem = fromString(jsThis.value.take(jsThis.value.size - 1))
          val layout = newLayout(elem)
          jsThis.value = ""
          values.now.append(layout)
          values.recalc()
        }

        if(key == KCode.Comma) doUpdate()

        if(key == KCode.Enter) doUpdate()

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

        def doUpdate() = {
          val elem = fromString(jsThis.value.take(jsThis.value.size - 1))
          val layout = newLayout(elem)
          jsThis.value = ""
          values.now += layout
          values.recalc()
        }

        if(key == KCode.Comma) doUpdate()

        if(key == KCode.Enter) doUpdate()

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

  object TextRx {
    def basic(mods: Modifier *) = input(mods).render
    def option[T](inputTag: TypedTag[dom.html.Input])(make: String => Option[T], unmake: T => String) = new OptionTextRx(inputTag)(make,unmake)
    //def autocomplete = ???
    def  set[T, Layout <: FormidableRx[T]](inputTag: TypedTag[dom.html.Input])(fromString: String => T)(newLayout: T => Layout) = new TextRxSet[T,Layout](inputTag)(fromString)(newLayout)
    def list[T, Layout <: FormidableRx[T]](inputTag: TypedTag[dom.html.Input])(fromString: String => T)(newLayout: T => Layout) = new TextRxBufferList[T,Layout](inputTag)(fromString)(newLayout)
  }

}
