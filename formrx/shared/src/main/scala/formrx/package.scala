package object formrx {
  case object FormidableUninitialized extends Throwable("Uninitialized Field")
  case object FormidableProcessingFailure extends Throwable("Formidable is actively processing")
}
