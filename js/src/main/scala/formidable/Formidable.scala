package formidable

import scala.language.experimental.macros

trait Formidable[Target] {
  def build(): Target
  def unbuild(inp: Target): Unit
}

object Formidable {
  def apply[Layout,Target]: Layout with Formidable[Target] = macro Macros.mk2[Layout,Target]
}
