package formidable

import scala.language.experimental.macros
import scala.util.Try

trait FormidableRx[Target] {
  def current: rx.Rx[Try[Target]]
  def set(inp: Target): Unit
  def reset(): Unit
}

object FormidableRx {
  def apply[Layout,Target]: Layout with FormidableRx[Target] = macro Macros.generate[Layout,Target]
}