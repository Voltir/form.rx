package formrx.implicits

import org.scalajs.dom
import org.scalajs.dom.html
import rx.Var

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.util.Try

trait BindDynamic[T] {
  private val KEY = "_inp_rx"
  protected def bindDynamic(inp: html.Input)(make: String => Try[T]): Var[Try[T]] = {
    val bound = inp.asInstanceOf[js.Dynamic].selectDynamic(KEY).asInstanceOf[UndefOr[Var[Try[T]]]]
    bound.getOrElse {
      val result: Var[Try[T]] = Var(make(inp.value))
      inp.addEventListener("keyup",(evt:dom.KeyboardEvent) => { result() = make(inp.value) })
      inp.addEventListener("paste",(evt:dom.KeyboardEvent) => js.timers.setTimeout(0){ result() = make(inp.value) })
      inp.addEventListener("cut",(evt:dom.KeyboardEvent) => js.timers.setTimeout(0){ result() = make(inp.value) })
      inp.asInstanceOf[js.Dynamic].updateDynamic(KEY)(result.asInstanceOf[js.Any])
      result
    }
  }
}
