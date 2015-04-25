package formidable.implicits

import formidable.BindRx
import formidable.Typeclasses.StringConstructable
import org.scalajs.dom
import org.scalajs.dom.html

import scala.scalajs.js
import scala.util.{Success, Try}

trait TextArea {
  //Helper trait to shove Rx[Try[T]] into a dom.html.Input
  trait TextAreaRxDynamic[T] {
    protected def bindDynamic(inp: dom.html.TextArea)(make: String => Try[T]): rx.Rx[Try[T]] = {
      if(inp.asInstanceOf[js.Dynamic].selectDynamic("_inp_rx") == js.undefined) {
        val result: rx.Rx[Try[T]] = rx.Rx {
          println("InputBindRx -- I AM WORRIED THIS LEAKS!")
          make(inp.value)
        }
        inp.asInstanceOf[js.Dynamic].updateDynamic("_inp_rx")(result.asInstanceOf[js.Any])
        result
      } else {
        inp.asInstanceOf[js.Dynamic]._inp_rx.asInstanceOf[rx.Rx[Try[T]]]
      }
    }
  }


  //Binder for dom.html.Input
  class TextAreaBindRx[Target: StringConstructable]
    extends BindRx[dom.html.TextArea,Target]
    with TextAreaRxDynamic[Target] {
    val builder = implicitly[StringConstructable[Target]]

    def bind(inp: dom.html.TextArea, value: Target): Unit = {
      inp.value = builder.asString(value)
      bindDynamic(inp)(s => builder.parse(inp.value)).recalc()
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

  class TextAreaOptionBindRx[Target: StringConstructable]
    extends BindRx[dom.html.TextArea,Option[Target]]
    with TextAreaRxDynamic[Option[Target]] {
    val builder = implicitly[StringConstructable[Target]]

    def bind(inp: dom.html.TextArea, value: Option[Target]): Unit = {
      inp.value = value.map(builder.asString).getOrElse("")
      bindDynamic(inp)(s => Success(builder.parse(inp.value).toOption)).recalc()
    }

    def unbind(inp: dom.html.TextArea): rx.Rx[Try[Option[Target]]] = {
      val result = bindDynamic(inp)(s => Success(builder.parse(inp.value).toOption))
      inp.onkeyup = (ev:dom.KeyboardEvent) => {
        result.recalc()
      }
      result
    }

    override def reset(inp: dom.html.TextArea): Unit = bind(inp,None)
  }

  implicit def textAreaOptionBindRx[Target: StringConstructable]: BindRx[dom.html.TextArea, Option[Target]] = new TextAreaOptionBindRx[Target]
}
