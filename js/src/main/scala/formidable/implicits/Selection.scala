package formidable.implicits

import formidable.FormidableRx
import formidable.Implicits.{Radio, Opt}
import org.scalajs.dom

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

    override def set(value: T): Unit = {
      select.selectedIndex = options.indexWhere(_.value == value)
      selected() = value
    }

    override def reset(): Unit = {
      select.selectedIndex = 0
      selected() = head.value
    }
  }

  object SelectionRx {
    def apply[T](selectMods: Modifier*)(head: Opt[T], options: Opt[T] *) = new SelectionRx[T](selectMods)(head, options:_*)
  }
}
