package formidable.implicits

object all
  extends Common
  with Input
  with TextArea
  with Checkbox
  with RadioRx
  with Selection {
  case object LoadFailure extends Throwable("Formidable is actively processing")
}


