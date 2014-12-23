package formidable

import utest._

object Foo extends TestSuite {

  def tests = TestSuite {
    'Foo { 
      println("FOO!")
    }
  }
}
