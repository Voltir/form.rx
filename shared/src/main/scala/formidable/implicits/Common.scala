package formidable.implicits

import formidable.{BindRx, FormidableRx}
import scala.util.{Success, Try}
import rx._

trait Common {

  //Binder for Ignored fields
  class Ignored[T](val default: T)(implicit ctx: RxCtx) extends FormidableRx[T] {
    override val current: Rx[Try[T]] = Rx { Success(default) }
    override def set(inp: T): Unit = ()
    override def reset(): Unit = ()
  }

  object Ignored {
    def apply[T](default: T)(implicit ctx: RxCtx) = new Ignored(default)
  }

  //Implicit for general FormidableRx
  class FormidableBindRx[F <: FormidableRx[Target],Target] extends BindRx[F,Target] {
    override def bind(inp: F, value: Target) = inp.set(value)
    override def unbind(inp: F): Rx[Try[Target]] = inp.current
    override def reset(inp: F): Unit = inp.reset()
  }
  implicit def implicitFormidableBindRx[F <: FormidableRx[Target],Target]: BindRx[F,Target] = new FormidableBindRx[F,Target]

  //Implicit for rx.Var binding
  class VarBindRx[Target](implicit ctx: RxCtx) extends BindRx[Var[Target],Target] {
    override def bind(inp: Var[Target], value: Target) = inp() = value
    override def unbind(inp: Var[Target]): rx.Rx[Try[Target]] = inp.map(a => scala.util.Try(a))

    //For resetting vars, we cheat. The Formidable macro itself does the reset. We must ignore this call here.
    override def reset(inp: Var[Target]): Unit = ()

  }
  implicit def implicitVarBindRx[Target](implicit ctx: RxCtx): BindRx[Var[Target],Target] = new VarBindRx[Target]()

  //Generic formidable to lift into lift a defined formidable into an Option type
  class FormidableOptionBindRx[Target, F <: FormidableRx[Target]](implicit ctx: RxCtx) extends BindRx[F,Option[Target]] {
    override def bind(inp: F, value: Option[Target]) = {
      value.foreach(inp.set)
      inp.current.recalc()
    }
    override def unbind(inp: F): Rx[Try[Option[Target]]] = Rx { Success(inp.current().toOption) }
    override def reset(inp: F): Unit = inp.reset()
  }
  implicit def implicitFormidableOptionBindRx[Target, F <: FormidableRx[Target]](implicit ctx: RxCtx): BindRx[F,Option[Target]] = new FormidableOptionBindRx[Target,F]

  //This class is for a List of FormidableRx's for variable sized form parts (ie: List of experience in a Resume form)
  class RxLayoutList[T, Layout <: FormidableRx[T]](make: () => Layout)(implicit ctx: RxCtx) extends FormidableRx[List[T]] {

    val values: rx.Var[collection.mutable.Buffer[Layout]] = rx.Var(collection.mutable.Buffer.empty)

    //override lazy val current: rx.Rx[Try[List[T]]] = values.map(ls => Try(ls.view.map(_.current()).flatten.toList))
    override lazy val current: rx.Rx[Try[List[T]]] = values.map(ls => Try(ls.view.map(_.current().get).toList))

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

    def append(): Unit = {
      values.now += make()
      values.propagate()
    }

    def append(t: T) = {
      val r = make()
      r.set(t)
      values.now += r
      values.propagate()
    }

    def remove(elem: Layout): Unit = {
      values.now -= elem
      values.propagate()
    }
  }
}
