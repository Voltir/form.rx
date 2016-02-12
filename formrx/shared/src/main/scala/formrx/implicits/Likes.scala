package formrx.implicits

import likelib.{StringLike, StringTryLike}

import scala.util.{Success, Try}

trait ReallyLowPriorityStringLikes {
  implicit def likeTryLike[T: StringLike]: StringTryLike[T] = new StringTryLike[T] {
    override def to(inp: T): String = implicitly[StringLike[T]].to(inp)
    override def from(txt: String): Try[T] = Success(implicitly[StringLike[T]].from(txt))
  }
}

trait LowPriorityStringLikes extends ReallyLowPriorityStringLikes {
  implicit val StringStringLike = new StringTryLike[String] {
    override def to(inp: String): String = inp
    override def from(txt: String): Try[String] = Success(txt)
  }

  implicit val IntStringLike = new StringTryLike[Int] {
    override def to(inp: Int): String = inp.toString
    override def from(txt: String): Try[Int] = Try(txt.toInt)
  }

  implicit val LongStringLike = new StringTryLike[Long] {
    override def to(inp: Long): String = inp.toString
    override def from(txt: String): Try[Long] = Try(txt.toLong)
  }

  implicit val FLoatStringLike = new StringTryLike[Float] {
    override def to(inp: Float): String = inp.toString
    override def from(txt: String): Try[Float] = Try(txt.toLong)
  }

  implicit val DoubleStringLike = new StringTryLike[Float] {
    override def to(inp: Float): String = inp.toString
    override def from(txt: String): Try[Float] = Try(txt.toLong)
  }

  implicit def OptLike[T: StringTryLike] = new StringTryLike[Option[T]] {
    override def to(inp: Option[T]) = inp.map(implicitly[StringTryLike[T]].to).getOrElse("")
    override def from(txt: String) = {
      if(txt.length == 0) scala.util.Success(Option.empty[T])
      else implicitly[StringTryLike[T]].from(txt).map(Option.apply)
    }
  }
}