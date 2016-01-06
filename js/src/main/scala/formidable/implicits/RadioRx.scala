package formidable.implicits

import rx._
import formidable._
import scala.util.Try

trait Radio {
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
  class RadioRx[T](name: String)(val head: Radio[T], val tail: Radio[T] *) extends FormidableRx[T] {
    private val selected: rx.Var[T] = rx.Var(head.value)

    override val current: rx.Rx[Try[T]] = selected.map(s => scala.util.Success(s))

    val radios = (head :: tail.toList).map { r =>
      r.input.name = name
      r.input.onchange = { (_:Event) => selected() = r.value }
      r
    }.toBuffer


    override def set(value: T): Unit = radios.find(_.value == value).foreach { r =>
      r.input.checked = true
      selected() = r.value
    }

    override def reset(): Unit = {
      set(head.value)
    }
  }

  class DynamicRadioRx[T](name: String)(radiosRx: Rx[List[Radio[T]]]) extends FormidableRx[T] {

    val current: Rx[Try[T]] = radiosRx.map(
      _.find(_.input.checked)
      .map(r => scala.util.Success(r.value))
      .getOrElse(scala.util.Failure(FormidableUninitialized))
    )

    def set(value: T) = {
      radiosRx.now.filter(_.value == value).foreach(_.input.checked = true)
    }

    def reset() = {
      radiosRx.now.foreach(_.input.checked = false)
    }

    private val watchRadios = radiosRx.foreach { rs =>
      rs.foreach { r =>
        r.input.name = name
        r.input.onchange = { (_: Event) => current.recalc() }
      }
    }
  }

  object RadioRx {
    def apply[T](name: String)(head: Radio[T], radios: Radio[T] *) = new RadioRx[T](name)(head,radios.toList:_*)
    def dynamic[T](name: String)(radiosRx: Rx[List[Radio[T]]]) = new DynamicRadioRx[T](name)(radiosRx)
  }
}
