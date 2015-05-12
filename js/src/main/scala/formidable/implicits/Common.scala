package formidable.implicits


import formidable.{BindRx, FormidableRx}
import scala.util.{Success, Try}
import rx._

trait Common {

  //Binder for Ignored fields
  class Ignored[T](val default: T) extends FormidableRx[T] {
    override def current: Rx[Try[T]] = Rx { Success(default) }
    override def set(inp: T): Unit = Unit
    override def reset(): Unit = Unit
  }

  object Ignored {
    def apply[T](default: T) = new Ignored(default)
  }

  //Implicit for general FormidableRx
  class FormidableBindRx[F <: FormidableRx[Target],Target] extends BindRx[F,Target] {
    override def bind(inp: F, value: Target) = inp.set(value)
    override def unbind(inp: F): rx.Rx[Try[Target]] = inp.current
    override def reset(inp: F): Unit = inp.reset()
  }
  implicit def implicitFormidableBindRx[F <: FormidableRx[Target],Target]: BindRx[F,Target] = new FormidableBindRx[F,Target]

  //Implicit for rx.Var binding
  class VarBindRx[Target] extends BindRx[Var[Target],Target] {
    import rx.ops._
    override def bind(inp: Var[Target], value: Target) = inp() = value
    override def unbind(inp: Var[Target]): rx.Rx[Try[Target]] = inp.map(a => scala.util.Try(a))

    //For resetting vars, we cheat. The Formidable macro itself does the reset. We must ignore this call here.
    override def reset(inp: Var[Target]): Unit = {
      println("RESET QUITE!")
      Unit
    }
  }
  implicit def implicitVarBindRx[Target]: BindRx[Var[Target],Target] = new VarBindRx[Target]()

  //This class is for a List of FormidableRx's for variable sized form parts (ie: List of experience in a Resume form)
  class RxLayoutList[T, Layout <: FormidableRx[T]](make: () => Layout) extends FormidableRx[List[T]] {

    val values: rx.Var[collection.mutable.Buffer[Layout]] = rx.Var(collection.mutable.Buffer.empty)

    override lazy val current: rx.Rx[Try[List[T]]] = rx.Rx { Try {
      values().map { l =>
        l.current().get
      }.toList
    }}

    override def set(inp: List[T]): Unit = {
      values() = inp.map { f =>
        val r = make()
        r.set(f)
        r
      }.toBuffer
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
