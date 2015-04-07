package formidable.implicits


import formidable.{BindRx, FormidableRx}
import scala.util.{Success, Try}
import rx._

trait Common {

  //Binder for Ignored fields
  class Ignored[T](val default: T) extends FormidableRx[T] {
    override def current: Rx[Try[T]] = Rx { Success(default) }
    override def unbuild(inp: T): Unit = Unit
  }

  object Ignored {
    def apply[T](default: T) = new Ignored(default)
  }

  //Implicit for general FormidableRx
  class FormidableBindRx[F <: FormidableRx[Target],Target] extends BindRx[F,Target] {
    override def bind(inp: F, value: Target) = inp.unbuild(value)
    override def unbind(inp: F): rx.Rx[Try[Target]] = inp.current
  }
  implicit def implicitFormidableBindRx[F <: FormidableRx[Target],Target]: BindRx[F,Target] = new FormidableBindRx[F,Target]

  //Implicit for binding arbitrary vars
  class BindVarRx[T]() extends BindRx[rx.Var[T],T] {
    override def bind(inp: rx.Var[T], value: T) = inp() = value
    override def unbind(inp: rx.Var[T]): rx.Rx[Try[T]] = rx.Rx { Try(inp()) }
  }
  implicit def implicitBindVarRx[Target]: BindRx[rx.Var[Target],Target] = new BindVarRx[Target]()

  //This class is for a List of FormidableRx's for variable sized form parts (ie: List of experience in a Resume form)
  class RxLayoutList[T, Layout <: FormidableRx[T]](make: () => Layout) extends FormidableRx[List[T]] {

    val values: rx.Var[collection.mutable.Buffer[Layout]] = rx.Var(collection.mutable.Buffer.empty)

    override lazy val current: rx.Rx[Try[List[T]]] = rx.Rx { Try {
      values().map { l =>
        l.current().get
      }.toList
    }}

    def unbuild(inp: List[T]): Unit = {
      values() = inp.map { f =>
        val r = make()
        r.unbuild(f)
        r
      }.toBuffer
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
