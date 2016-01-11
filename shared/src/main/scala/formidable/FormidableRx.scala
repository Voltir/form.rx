package formidable

import rx.RxCtx

import scala.language.experimental.macros
import scala.reflect.ClassTag
import scala.util.Try

trait FormidableRx[Target] {
  def current: rx.Rx[Try[Target]]
  def set(inp: Target): Unit
  def reset(): Unit
}

trait LayoutFor[Target]

object FormidableRx {
  def apply[Layout,Target]: Layout with FormidableRx[Target] = macro Macros.generate[Layout,Target]

  def apply2[T,Layout <: LayoutFor[T]](implicit ctx: RxCtx): Layout with FormidableRx[T] = macro Macros2.generate[T,Layout]
}

