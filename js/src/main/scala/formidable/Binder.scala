package formidable
import scala.util.Try

trait Binder[I,O] {
  def bind(inp: I, value: O): Unit
  def unbind(inp: I): Try[O]
}
