package formidable

import scala.language.experimental.macros
import scala.util.Try

trait Formidable[Target] {
  def build(): Try[Target]
  def unbuild(inp: Target): Unit
}

object Formidable {
  def apply[Layout,Target]: Layout with Formidable[Target] = macro Macros.generate[Layout,Target]
}

trait FormidableRx[Target] {
  def current: rx.Rx[Try[Target]]
  def set(inp: Target, propagate: Boolean = true): Unit
  def reset(propagate: Boolean = true): Unit
}

object FormidableRx {
  def apply[Layout,Target]: Layout with FormidableRx[Target] = macro MacrosNext.generate[Layout,Target]
}