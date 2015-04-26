package formidable.implicits


import formidable.{BindRx, FormidableRx}
import scala.util.{Success, Try}
import rx._

trait Common {

  //Binder for Ignored fields
  class Ignored[T](val default: T) extends FormidableRx[T] {
    override def current: Rx[Try[T]] = Rx { Success(default) }
    override def set(inp: T, propagate: Boolean): Unit = Unit
    override def reset(): Unit = Unit
  }

  object Ignored {
    def apply[T](default: T) = new Ignored(default)
  }

  //Implicit for general FormidableRx
  class FormidableBindRx[F <: FormidableRx[Target],Target] extends BindRx[F,Target] {
    override def bind(inp: F, value: Target, propagate: Boolean) = inp.set(value, propagate)
    override def unbind(inp: F): rx.Rx[Try[Target]] = inp.current
    override def reset(inp: F): Unit = inp.reset()
  }
  implicit def implicitFormidableBindRx[F <: FormidableRx[Target],Target]: BindRx[F,Target] = new FormidableBindRx[F,Target]

  class FormidableVarRx[T](default: T) extends FormidableRx[T] {
    val value = Var(default)
    override def current = Rx { Try(value()) }
    override def set(inp: T, propagate: Boolean) = if(propagate) value.update(inp) else value.updateSilent(inp)
    override def reset(): Unit = value() = default
  }

  object VarRx {
    def apply[Target](default: Target) = new FormidableVarRx[Target](default)
  }
  //This class is for a List of FormidableRx's for variable sized form parts (ie: List of experience in a Resume form)
  class RxLayoutList[T, Layout <: FormidableRx[T]](make: () => Layout) extends FormidableRx[List[T]] {

    val values: rx.Var[collection.mutable.Buffer[Layout]] = rx.Var(collection.mutable.Buffer.empty)

    override lazy val current: rx.Rx[Try[List[T]]] = rx.Rx { Try {
      values().map { l =>
        l.current().get
      }.toList
    }}

    override def set(inp: List[T], propagate: Boolean): Unit = {
      values.updateSilent(inp.map { f =>
        val r = make()
        r.set(f)
        r
      }.toBuffer)
      if(propagate) values.propagate()
    }

    override def reset(): Unit = {
      values() = collection.mutable.Buffer.empty
    }

    def append(elem: Layout): Unit = {
      values.now += elem
      values.recalc()
    }

    def remove(elem: Layout): Unit = {
      values.now -= elem
      values.recalc()
    }
  }
}
