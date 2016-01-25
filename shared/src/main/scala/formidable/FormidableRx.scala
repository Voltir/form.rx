package formidable

import scala.language.experimental.macros
import scala.util.Try

trait FormidableRx[Target] {
  def current: rx.Rx[Try[Target]]
  def set(inp: Target): Unit
  def reset(): Unit
}

object FormidableRx {
  def apply[T,Layout](implicit ctx: rx.Ctx.Owner): Layout with FormidableRx[T] = macro Macros.generate[T,Layout]
}

