package formidable.implicits

import formidable.FormidableRx
import formidable.Implicits.Radio
import scala.util.Try


trait RadioRx {
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

  object RadioRx {
    def apply[T](name: String)(radios: Radio[T] *) = new RadioRx[T](name)(radios:_*)
  }
}
