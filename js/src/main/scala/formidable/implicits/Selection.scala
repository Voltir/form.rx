package formidable.implicits

import formidable.FormidableRx
import formidable.Implicits.{Radio, Opt}
import org.scalajs.dom

import scala.util.Try
import scalatags.JsDom.all._

trait Selection {
  //Binders for T <=> Select element
  class SelectionRx[T](selectMods: Modifier *)(options: Opt[T] *) extends FormidableRx[T] {
    private val values = options.map(_.value).toBuffer
    private val selected: rx.Var[T] = rx.Var(options.head.value)

    val select: dom.html.Select = scalatags.JsDom.all.select(
      scalatags.JsDom.all.onchange:={() => selected() = values(select.selectedIndex) }
    )(selectMods)(options.map(_.option):_*).render

    val current: rx.Rx[Try[T]] = rx.Rx {
      Try(selected())
    }

    override def unbuild(value: T): Unit = {
      select.selectedIndex = options.indexWhere(_.value == value)
      selected() = value
    }
  }

  object SelectionRx {
    def apply[T](selectMods: Modifier*)(options: Opt[T] *) = new SelectionRx[T](selectMods)(options:_*)
    //def mapped[T,U](func: T => U, selectMods: Modifier*)(options: Opt[T] *) = new SelectionRx[T](selectMods)(options:_*).mapped(func)
  }

  //Binders for T <=> Radio elements
  class RadioRx[T](name: String)(val radios: Radio[T] *) extends FormidableRx[T] {
    private val selected: rx.Var[T] = rx.Var(radios.head.value)
    radios.foreach(_.input.name = name)

    val current: rx.Rx[Try[T]] = rx.Rx { Try(selected()) }

    def unbuild(value: T): Unit = radios.find(_.value == value).foreach { r =>
      r.input.checked = true
      selected() = r.value
    }
  }
}
