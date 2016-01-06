package formidable

trait BindRx[I,O] {
  def bind(inp: I, value: O): Unit
  def unbind(inp: I): rx.Node[scala.util.Try[O]]
  def reset(inp: I): Unit
}