package formidable

import scala.util.Try

object Typeclasses {
  trait StringConstructable[Target] {
    def asString(inp: Target): String
    def parse(txt: String): Try[Target]
  }
}
