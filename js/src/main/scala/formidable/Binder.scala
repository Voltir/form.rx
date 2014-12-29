package formidable

trait Binder[I,O] {
  def bind(inp: I, value: O): Unit
  def unbind(inp: I): O
}
