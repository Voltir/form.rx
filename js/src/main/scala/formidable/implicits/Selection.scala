package formidable.implicits

import formidable.FormidableRx
import formidable.Implicits.{Radio, Opt}
import org.scalajs.dom

import rx._
import scala.util.Try
import scalatags.JsDom.all._

trait Selection {
  //Binders for T <=> Select element
  class SelectionRx[T](selectMods: Modifier *)(head: Opt[T], options: Opt[T] *) extends FormidableRx[T] {
    private val values = collection.mutable.Buffer(head.value) ++ options.map(_.value).toBuffer
    private val selected: rx.Var[T] = rx.Var(options.head.value)

    val select: dom.html.Select = scalatags.JsDom.all.select(
      scalatags.JsDom.all.onchange:={() => selected() = values(select.selectedIndex) }
    )(selectMods)(options.map(_.option):_*).render

    override val current: rx.Rx[Try[T]] = rx.Rx {
      Try(selected())
    }

    override def set(value: T, propagate: Boolean): Unit = {
      select.selectedIndex = options.indexWhere(_.value == value)
      selected.updateSilent(value)
      if(propagate) selected.propagate()
    }

    override def reset(): Unit = {
      select.selectedIndex = 0
      selected() = head.value
    }
  }

  //Select element with dynamic (ie Rx'ing) set of choices
  class DynamicSelectionRx[T](onReset: Boolean => Unit, selectMods: Modifier *)(optionsRx: Rx[List[Opt[T]]]) extends FormidableRx[T] {
    private val selectedIndex: Var[Int] = Var(0)

    val select: dom.html.Select = scalatags.JsDom.all.select(
      selectMods,
      scalatags.JsDom.all.onchange := { () => selectedIndex() = select.selectedIndex }
    ).render

    val current: Rx[Try[T]] = Rx {
      Try(optionsRx.now(selectedIndex()).value)
    }

    override def set(value: T, propagate: Boolean): Unit = {
      selectedIndex.updateSilent(optionsRx.now.indexOf(value))
      if(propagate) selectedIndex.propagate()
    }

    override def reset(): Unit = {
      selectedIndex() = 0
      onReset(true) //todo add propagate to reset
    }

    private val watchOptions = Obs(optionsRx) {
      (0 until select.childElementCount).foreach { _ => select.remove(0) }
      optionsRx.now.foreach { opt =>
        select.add(opt.option.render)
      }
      if(optionsRx.now.size <= selectedIndex.now) selectedIndex() = 0
      else {
        select.selectedIndex = selectedIndex.now
        current.recalc()
      }
    }
  }

  object SelectionRx {
    def apply[T](selectMods: Modifier*)(head: Opt[T], options: Opt[T] *) = new SelectionRx[T](selectMods)(head, options:_*)
    def dynamic[T](onReset: Boolean => Unit, selectMods: Modifier *)(options: Rx[List[Opt[T]]]) =
      new DynamicSelectionRx[T](onReset,selectMods)(options)
  }
}
