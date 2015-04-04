package formidable.implicits


import formidable.FormidableRx
import scala.util.Try
import rx._

trait Common {

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
