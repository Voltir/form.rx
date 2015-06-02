package formidable.implicits

import formidable._
import scala.util.Try

trait RadioRx {
  import org.scalajs.dom._
  import scalatags.JsDom.all.{html => _, _}

  //Wrapper for radio type inputs
  class Radio[+T](val value: T)(mods: Modifier *) {
    val input = scalatags.JsDom.all.input(`type`:="radio",mods).render
  }

  object Radio {
    def apply[T](value: T)(mods: Modifier *) = new Radio(value)(mods)
  }

  //Binders for T <=> Radio elements
  class RadioRx[T](name: String)(val head: Radio[T], val radios: Radio[T] *) extends FormidableRx[T] {
    private val selected: rx.Var[T] = rx.Var(head.value)
    private val _all = head :: radios.toList
    _all.foreach(_.input.name = name)

    override val current: rx.Rx[Try[T]] = rx.Rx { Try(selected()) }

    override def set(value: T): Unit = _all.find(_.value == value).foreach { r =>
      r.input.checked = true
      selected() = r.value
    }

    override def reset(): Unit = {
      set(head.value)
    }
  }

  object RadioRx {
    def apply[T](name: String)(head: Radio[T], radios: Radio[T] *) = new RadioRx[T](name)(head,radios.toList:_*)
  }
}
