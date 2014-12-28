package formidable

import scalatags.JsDom.all._
import scala.language.experimental.macros

trait Formidable[Target] {
  def populate(inp: Target): Unit
  def construct(): Target
}

object Formidable {
  def apply[Layout,Target]: Layout with formidable.Formidable[Target] = macro Macros.mk2[Layout,Target]
}
