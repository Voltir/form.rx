package formidable.implicits

import formidable.BindRx
import formidable.Typeclasses.StringConstructable
import org.scalajs.dom
import org.scalajs.dom.html
import rx._

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.util.{Success, Try}

trait TextArea {
  //Helper trait to shove Rx[Try[T]] into a dom.html.Input
  trait TextAreaRxDynamic[T] {
    private val KEY = "_inp_rx"
    protected def bindDynamic(inp: html.TextArea)(make: String => Try[T]): Var[Try[T]] = {
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
  class TextAreaBindRx[Target: StringConstructable]
    extends BindRx[dom.html.TextArea,Target]
    with TextAreaRxDynamic[Target] {
    val builder = implicitly[StringConstructable[Target]]

    def bind(inp: dom.html.TextArea, value: Target, propagate: Boolean): Unit = {
      inp.value = builder.asString(value)
      val dynamicRx = bindDynamic(inp)(s => builder.parse(inp.value))
      if(propagate) dynamicRx.recalc()
    }

    def unbind(inp: dom.html.TextArea): rx.Rx[Try[Target]] = {
      val result = bindDynamic(inp)(s => builder.parse(inp.value))
      inp.onkeyup = (ev:dom.KeyboardEvent) => result.recalc()
      result
    }

    def reset(inp: dom.html.TextArea): Unit = {
      inp.value = ""
      bindDynamic(inp)(s => builder.parse(inp.value)).recalc()
    }
  }

  implicit def textAreaBindRx[Target: StringConstructable]: BindRx[dom.html.TextArea,Target] = new TextAreaBindRx[Target]
}
