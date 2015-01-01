package formidable

import scala.language.experimental.macros
import scala.util.Try

trait Formidable[Target] {
  def build(): Try[Target]
  def unbuild(inp: Target): Unit
}

object Formidable {
  def apply[Layout,Target]: Layout with Formidable[Target] = macro Macros.mk2[Layout,Target]
}
