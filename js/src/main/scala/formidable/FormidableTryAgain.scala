package formidable

import scalatags.JsDom.all._

object FormidableTryAgain {

  class Formidable[T](mapper: Mapper[T]) {
    def populate(inp: T): Unit = mapper.populate(inp)
    def construct(): T = mapper.construct()
  }

  trait Mapper[T] {
    def populate(inp: T): Unit
    def construct(): T
  }

  class InputField extends Mapper[String] {
    val field = scalatags.JsDom.tags.input(`type`:="text").render
    def populate(inp: String): Unit = field.value = inp
    def construct(): String = field.value
  }

  object Internal {
    class Mapper2[T,A1,A2]
      (f1: (String,Mapper[A1]),f2: (String,Mapper[A2]))
      (apply: (A1,A2) => T)
      (unapply: T => Option[(A1,A2)])
      extends Mapper[T] {

      override def populate(inp: T) = unapply(inp).map { case (a1,a2) =>
          f1._2.populate(a1)
          f2._2.populate(a2)
      }
      override def construct(): T = {
        apply(f1._2.construct,f2._2.construct)
      }

    }
  }
  object Mapper {
    import FormidableTryAgain.Internal._
    def apply[T,A1,A2]
      (f1: (String,Mapper[A1]),f2: (String,Mapper[A2]))
      (apply: (A1,A2) => T)
      (unapply: T => Option[(A1,A2)]): Mapper[T] = new Mapper2(f1,f2)(apply)(unapply)
  }
}
