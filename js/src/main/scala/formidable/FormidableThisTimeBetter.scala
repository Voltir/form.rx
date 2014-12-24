package formidable

import scalatags.JsDom.all._

object FormidableThisTimeBetter {

  trait Formidable[Target] {
    def populate(inp: Target): Unit
    //def construct(): Target
  }

  class TestFormidable[Target] extends Formidable[Target]{
    def populate(inp: Target): Unit = println("TEST POPULATE")
  }
}
