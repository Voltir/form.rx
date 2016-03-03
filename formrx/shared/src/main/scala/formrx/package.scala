package object formrx {

  case object FormidableUninitialized extends Throwable("Uninitialized Field")

  case object FormidableProcessingFailure extends Throwable("Formidable is actively processing")

  trait Procs {
    def proc(): Unit
  }

  trait FormProcs[T] extends Procs { self: FormRx[T] =>
    def getOrProc(f: T => Unit): Unit = self.current.now.toOption.fold(proc())(f)
  }
}
