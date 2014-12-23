package formidable.validators

import formidable.Validator

object any {

  object AnyString extends Validator[String, String] {
    override def validate(inp: String) = true
    override def build(inp: String): String = inp
    override def unbuild(inp: String): String = inp
  }

  object AnyInt extends Validator[String, Int] {
    override def validate(inp: String) = inp.toInt.isInstanceOf[Int]
    override def build(inp: String): Int = inp.toInt
    override def unbuild(inp: Int): String = inp.toString
  }
}
