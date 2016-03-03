package formrx

import scala.language.experimental.macros
import scala.util.Try

trait FormRx[Target] {
  def current: rx.Rx[Try[Target]]
  def set(inp: Target): Unit
  def reset(): Unit
}

object FormRx {
  def apply[T,Layout](implicit ctx: rx.Ctx.Owner): Layout with FormRx[T] with FormProcs[T] = macro Macros.generate[T,Layout]
}

