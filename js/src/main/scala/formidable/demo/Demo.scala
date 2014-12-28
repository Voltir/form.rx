package formidable.demo

import formidable._
import formidable.Implicits._
import org.scalajs.dom

import scala.scalajs.js.JSApp
import scalatags.JsDom.all._

object Demo1 {

  case class UserPass(name: String, password: String)

  trait UserPassLayout {
    val name = input(`type`:="text").render
    val password = input(`type`:="password").render
  }
}

object Demo2 {
  case class Inner(foo: String, bar: Int)
  case class Nested(top: String, inner: Inner, other: Inner)

  trait InnerLayout {
    val foo = input(`type`:="text").render
    val bar = new SelectWith[Int](
      Opt(1)(value:="One","One"),
      Opt(2)(value:="Two","twwo"),
      Opt(42)(value:="Life","Fizzle"),
      Opt(5)(value:="Five","5ive")
    )
  }
  trait NestedLayout {
    val top = input(`type`:="text").render
    val inner = Formidable[InnerLayout,Inner]
    val other = Formidable[InnerLayout,Inner]
  }
}

object DemoApp extends JSApp {

  def row: HtmlTag = div(cls:="row")

  def column(classes: String): HtmlTag = div(cls:=s"column $classes")

  def template[T]
      (title: String, description: String)
      (formidable: Formidable[T], defaultTxt: String, default: T)
      (formTag: HtmlTag): HtmlTag = {
    val created = div("Not created yet").render
    row(
      column("small-3")(
        row(column("small-12")(h3(title))),
        row(column("small-12")(description)),
        row(column("small-12")("Auto fill with ")(a(
          href:="#",
          defaultTxt,
          onclick := {() => formidable.populate(default)}
        ))),
        row(column("small-12")(created))
      ),
      column("small-9")(
        formTag(
          input(`type`:="Submit"),
          onsubmit := {() =>
            val txt = s"${formidable.construct()}"
            created.innerHTML = txt
            false
          }
        )
      )
    )
  }

  def first: HtmlTag = {
    val form1 = Formidable[Demo1.UserPassLayout,Demo1.UserPass]
    val default = Demo1.UserPass("Bob!","supersecretbob")
    template("Example 1","Basic User/Password form")(form1,"Bob",default) {
      form(
        form1.name,
        form1.password
      )
    }
  }

  def second: HtmlTag = {
    import Demo2._
    val form2 = Formidable[NestedLayout,Nested]
    val default = Nested("This is top",Inner("This is foo",2),Inner("Other foo",5))
    template("Example 2", "Formidable can nest")(form2,"Default",default) {
      form(
        form2.top,
        label("Inner:"),
        form2.inner.foo,
        form2.inner.bar.select,
        label("Other:"),
        form2.other.foo,
        form2.other.bar.select
      )
    }
  }

  def main(): Unit = {
    val content = dom.document.getElementById("content")
    content.appendChild(row(column("small-12 text-center")(h1("Example Forms"))).render)
    content.appendChild(Seq(first,hr).render)
    content.appendChild(Seq(second,hr).render)
  }
}
