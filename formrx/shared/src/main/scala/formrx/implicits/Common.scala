package formrx.implicits

import formrx.{BindRx, FormRx}
import scala.util.{Success, Try}
import rx._

trait Common {

  //Binder for Ignored fields
  class Ignored[T](val default: T)(implicit ctx: Ctx.Owner) extends FormRx[T] {
    override val current: Rx[Try[T]] = Rx { Success(default) }
    override def set(inp: T): Unit = ()
    override def reset(): Unit = ()
  }

  object Ignored {
    def apply[T](default: T)(implicit ctx: Ctx.Owner) = new Ignored(default)
  }

  //Implicit for general FormRx
  class FormidableBindRx[F <: FormRx[Target],Target] extends BindRx[F,Target] {
    override def bind(inp: F, value: Target) = inp.set(value)
    override def unbind(inp: F): Rx[Try[Target]] = inp.current
    override def reset(inp: F): Unit = inp.reset()
  }
  implicit def implicitFormidableBindRx[F <: FormRx[Target],Target]: BindRx[F,Target] = new FormidableBindRx[F,Target]

  //Implicit for rx.Var binding
  class VarBindRx[Target](implicit ctx: Ctx.Owner) extends BindRx[Var[Target],Target] {
    override def bind(inp: Var[Target], value: Target) = inp() = value
    override def unbind(inp: Var[Target]): rx.Rx[Try[Target]] = inp.map(a => scala.util.Try(a))

    //For resetting vars, we cheat. The Formidable macro itself does the reset. We must ignore this call here.
    override def reset(inp: Var[Target]): Unit = ()

  }
  implicit def implicitVarBindRx[Target](implicit ctx: Ctx.Owner): BindRx[Var[Target],Target] = new VarBindRx[Target]()

  //Implicit for rx.Dynamic binding
  class DynamicBindRx[Target](implicit ctx: Ctx.Owner) extends BindRx[Rx.Dynamic[Target], Target] {
    //Using an Rx.Dynamic as a Bind means it can't be set/reset - it should be used like a mapping operation
    override def bind(inp: Rx.Dynamic[Target], value: Target) = ()
    override def reset(inp: Rx.Dynamic[Target]): Unit = ()
    override def unbind(inp: Rx.Dynamic[Target]): rx.Rx[Try[Target]] = inp.map((a:Target) => Try(a))
  }
  implicit def implicitRxBindRx[Target](implicit ctx: Ctx.Owner): BindRx[Rx.Dynamic[Target],Target] = new DynamicBindRx[Target]()

  //Generic formidable to lift into lift a defined formidable into an Option type
  class FormidableOptionBindRx[Target, F <: FormRx[Target]](implicit ctx: Ctx.Owner) extends BindRx[F,Option[Target]] {
    override def bind(inp: F, value: Option[Target]) = {
      value.foreach(inp.set)
      inp.current.recalc()
    }
    override def unbind(inp: F): Rx[Try[Option[Target]]] = Rx { Success(inp.current().toOption) }
    override def reset(inp: F): Unit = inp.reset()
  }
  implicit def implicitFormidableOptionBindRx[Target, F <: FormRx[Target]](implicit ctx: Ctx.Owner): BindRx[F,Option[Target]] = new FormidableOptionBindRx[Target,F]

  //This class is for a List of FormRx's for variable sized form parts (ie: List of experience in a Resume form)
  class RxLayoutList[T, Layout <: FormRx[T] with formrx.Procs](make: Ctx.Owner => Layout)(implicit ctx: Ctx.Owner)
      extends FormRx[List[T]] with formrx.Procs {

    val values: rx.Var[collection.mutable.Buffer[Layout]] = rx.Var(collection.mutable.Buffer.empty)

    //override lazy val current: rx.Rx[Try[List[T]]] = values.map { a => Try(a.flatMap(_.current().toOption).toList) }
    override lazy val current: rx.Rx[Try[List[T]]] = Rx { Try(values().map(_.current().get).toList) }

    override def set(inp: List[T]): Unit = {
      values() = inp.view.map { f =>
        val r = make(ctx)
        r.set(f)
        r
      }.toBuffer
    }

    override def reset(): Unit = {
      values() = collection.mutable.Buffer.empty
    }

    def append(): Unit = {
      values.now += make(ctx)
      values.propagate()
    }

    def append(t: T) = {
      val r = make(ctx)
      r.set(t)
      values.now += r
      values.propagate()
    }

    def remove(elem: Layout): Unit = {
      values.now -= elem
      values.propagate()
    }

    override def proc(): Unit = {
      values.now.foreach(_.proc())
    }
  }
}
