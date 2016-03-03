package formrx.implicits

import formrx.{BindRx, KCode, KeyboardPolyfill, FormRx}
import likelib.StringTryLike
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

  //Binder for dom.html.Input
  class InputBindRx[Target: StringTryLike]
      extends BindRx[dom.html.Input,Target]
      with BindDynamic[Target] {
    
    private lazy val strLike = implicitly[StringTryLike[Target]]

    private def update(inp: dom.html.Input): Unit = {
      val dynamicVar = bindDynamic(inp)(strLike.from)
      dynamicVar() = strLike.from(inp.value)
    }

    override def bind(inp: dom.html.Input, value: Target): Unit = {
      inp.value = strLike.to(value)
      update(inp)
    }

    override def unbind(inp: dom.html.Input): rx.Rx[Try[Target]] = {
      bindDynamic(inp)(strLike.from)
    }

    override def reset(inp: dom.html.Input): Unit = {
      inp.value = ""
      update(inp)
    }
  }

  implicit def inputBindRx[Target: StringTryLike]: BindRx[dom.html.Input,Target] = new InputBindRx[Target]

  //For List of Things (ie Tag Like)
  class TextRxBufferList[T, Layout <: FormRx[T]]
      (inputTag: TypedTag[dom.html.Input])
      (fromString: String => T)
      (newLayout: () => Layout)
      (implicit ctx: Ctx.Owner) extends FormRx[List[T]] {

    val values: rx.Var[mut.Buffer[Layout]] = rx.Var(mut.Buffer.empty)

    def pop() = values() = values.now - values.now.last

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

    protected def handleKeyInput(inp: dom.html.Input): dom.KeyboardEvent => Unit = { evt =>
      val key = evt.polyfill()._1
      def doUpdate() = {
        evt.stopPropagation()
        evt.preventDefault()
        val elem = fromString(inp.value)
        val layout = newLayout()
        layout.set(elem)
        inp.value = ""
        values.now.append(layout)
        values.propagate()
      }

      if(key == KCode.Comma) doUpdate()

      if(key == KCode.Enter) doUpdate()

      if(key == KCode.Backspace && inp.value == "" && values.now.nonEmpty) {
        pop()
      }
    }

    lazy val input: dom.html.Input = {
      val result = inputTag(`type`:="text").render
      val delayed = (evt:dom.KeyboardEvent) => js.timers.setTimeout(0)(handleKeyInput(result)(evt))
      result.addEventListener[dom.KeyboardEvent]("keydown", handleKeyInput(result))
      result.addEventListener[dom.KeyboardEvent]("paste",delayed)
      result.addEventListener[dom.KeyboardEvent]("cut", delayed)
      result
    }
  }

  //For Set of Things (ie Tag Like)
  class TextRxSet[T, Layout <: FormRx[T]]
      (val inputTag: TypedTag[dom.html.Input])
      (val fromString: String => T)
      (val newLayout: () => Layout)
      (implicit ctx: Ctx.Owner) extends FormRx[Set[T]] {

    val values: rx.Var[Set[Layout]] = rx.Var(Set.empty)

    def pop() = values() = values.now - values.now.last

    val current: rx.Rx[Try[Set[T]]] = rx.Rx { Try {
      values().flatMap(_.current().toOption)
    }}

    override def set(newValues: Set[T]) = {
      values.now.foreach { r => r.current.kill() }
      values() = newValues.map { t => val r = newLayout() ; r.set(t) ; r }
    }

    override def reset(): Unit = set(Set.empty)

    protected def handleKeyInput(inp: dom.html.Input): dom.KeyboardEvent => Unit = { evt =>
      val key = evt.polyfill()._1

      def doUpdate(): Unit = {
        evt.stopPropagation()
        evt.preventDefault()
        val elem = fromString(inp.value)
        if(!current.now.toOption.exists(_.contains(elem))) {
          val layout = newLayout()
          layout.set(elem)
          inp.value = ""
          values() = values.now + layout
        }
      }

      if(key == KCode.Comma) doUpdate()

      if(key == KCode.Enter) doUpdate()

      if(key == KCode.Backspace && inp.value == "" && values.now.nonEmpty) {
        pop()
      }
    }

    lazy val input: dom.html.Input = {
      val result = inputTag(`type`:="text").render
      val delayed = (evt:dom.KeyboardEvent) => js.timers.setTimeout(0)(handleKeyInput(result)(evt))
      result.addEventListener[dom.KeyboardEvent]("keydown", handleKeyInput(result))
      result.addEventListener[dom.KeyboardEvent]("paste", delayed)
      result.addEventListener[dom.KeyboardEvent]("cut", delayed)
      result
    }

  }

  class Validate[T: StringTryLike]
      (defaultToUninitialized: Boolean)
      (mods: Modifier*)
      (implicit ctx: Ctx.Owner) extends FormRx[T] with formrx.Procs {

    private val strLike = implicitly[StringTryLike[T]]

    private lazy val defaultValue =
      if(defaultToUninitialized) Failure(formrx.FormidableUninitialized)
      else strLike.from("")

    private val _current: rx.Var[Try[T]] = rx.Var(defaultValue)

    val current: rx.Rx[Try[T]] = rx.Rx(_current())

    lazy val input: org.scalajs.dom.html.Input = {
      val result = scalatags.JsDom.all.input(`type` := "text", mods).render
      val delayed = (evt:dom.KeyboardEvent) => js.timers.setTimeout(0)(proc())
      result.addEventListener[dom.KeyboardEvent]("keyup", (_:dom.Event) => proc())
      result.addEventListener[dom.KeyboardEvent]("paste", delayed)
      result.addEventListener[dom.KeyboardEvent]("cut", delayed)
      result
    }

    override def set(inp: T) = {
      input.value = strLike.to(inp)
      _current() = Success(inp)
    }

    override def reset(): Unit = {
      input.value = ""
      _current() = defaultValue
    }

    override def proc(): Unit = {
      _current() = strLike.from(input.value)
    }
  }

  object InputRx {
    //def autocomplete = ???
    def validate[T: StringTryLike]
        (defaultToUninitialized: Boolean)
        (mods: Modifier *)
        (implicit ctx: Ctx.Owner) =
      new Validate[T](defaultToUninitialized)(mods)

    def set[T, Layout <: FormRx[T]]
        (inputTag: TypedTag[dom.html.Input])
        (fromString: String => T)
        (newLayout: () => Layout)
        (implicit ctx: Ctx.Owner) =
      new TextRxSet[T,Layout](inputTag)(fromString)(newLayout)

    def list[T, Layout <: FormRx[T]]
        (inputTag: TypedTag[dom.html.Input])
        (fromString: String => T)
        (newLayout: () => Layout)
        (implicit ctx: Ctx.Owner) =
      new TextRxBufferList[T,Layout](inputTag)(fromString)(newLayout)
  }

}
