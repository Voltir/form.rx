package formidable.implicits

import formidable.FormidableRx
import org.scalajs.dom
import rx._
import scala.util.Try
import scalatags.JsDom.all._

trait Selection {
  //Binders for T <=> Select element
  class Opt[+T](val value: T)(mods: Modifier *) {
    val option = scalatags.JsDom.all.option(mods)
  }

  object Opt {
    def apply[T](value: T)(mods: Modifier *) = new Opt(value)(mods)
  }

  class SelectionRx[T]
      (selectMods: Modifier *)
      (head: Opt[T], tail: Opt[T] *)
      (implicit ctx: Ctx.Owner) extends FormidableRx[T] {
    private val options = collection.mutable.Buffer(head) ++ tail.toBuffer
    private val selected: rx.Var[T] = rx.Var(head.value)

    val select: dom.html.Select = scalatags.JsDom.all.select(
      scalatags.JsDom.all.onchange:={() => selected() = options(select.selectedIndex).value }
    )(selectMods)(options.map(_.option):_*).render

    override val current: rx.Rx[Try[T]] = rx.Rx {
      Try(selected())
    }

    override def set(value: T): Unit = {
      select.selectedIndex = options.indexWhere(_.value == value)
      selected() = value
    }

    override def reset(): Unit = {
      select.selectedIndex = 0
      selected() = head.value
    }
  }

  //Select element with dynamic (ie Rx'ing) set of choices
  class DynamicSelectionRx[T]
      (onReset: () => Unit, selectMods: Modifier *)
      (optionsRx: Rx[List[Opt[T]]])
      (implicit ctx: Ctx.Owner) extends FormidableRx[T] {
    private val selectedIndex: Var[Int] = Var(0)

    val select: dom.html.Select = scalatags.JsDom.all.select(
      selectMods,
      scalatags.JsDom.all.onchange := { () => selectedIndex() = select.selectedIndex }
    ).render

    val current: Rx[Try[T]] = Rx {
      Try(optionsRx.now(selectedIndex()).value)
    }

    override def set(value: T): Unit = {
      val setIndex = optionsRx.now.indexWhere(_.value == value)
      if(setIndex >= 0) {
        selectedIndex() = setIndex
        select.selectedIndex = setIndex
      }
    }

    override def reset(): Unit = {
      selectedIndex() = 0
      onReset()
    }

    private val watchOptions = optionsRx.foreach { now =>
      (0 until select.childElementCount).foreach { _ => select.remove(0) }
      now.foreach { opt =>
        select.add(opt.option.render)
      }
      if(now.size <= selectedIndex.now) selectedIndex() = 0
      else {
        select.selectedIndex = selectedIndex.now
        current.recalc()
      }
    }
  }

  object SelectionRx {
    def apply[T](selectMods: Modifier*)(head: Opt[T], options: Opt[T] *)(implicit ctx: Ctx.Owner) =
      new SelectionRx[T](selectMods)(head, options:_*)

    def dynamic[T](onReset: () => Unit, selectMods: Modifier *)(options: Rx[List[Opt[T]]])(implicit ctx: Ctx.Owner) =
      new DynamicSelectionRx[T](onReset,selectMods)(options)
  }
}
