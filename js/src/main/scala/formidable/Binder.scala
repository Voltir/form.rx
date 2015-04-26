package formidable
import scala.util.Try

trait Binder[I,O] {
  def bind(inp: I, value: O): Unit
  def unbind(inp: I): Try[O]
}

trait BindRx[I,O] {
  def bind(inp: I, value: O, propagate: Boolean = true): Unit
  def unbind(inp: I): rx.Rx[Try[O]]
  def reset(inp: I): Unit
}
