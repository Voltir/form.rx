Form.rx
=======
API to map case classes to/from a 'layout' of HTMLInput elements and Scala.Rx Vars. It is akin in purpose to the playframework form library, but entirely done in Scala.js... and a rather different approach in API. 

Contents
========
- [Getting Started](#getting-started)
- [Quick Demo](#quick-demo)
- [Using Form.rx](#using-formrx)
  - [Form.rx Basics](#formrx-basics)
  - [Form.rx Nesting](#formrx-nesting)
  - [Using rx.Vars](#using-rxvars)
  - [Lists and Sets](#lists-and-sets)

Getting Started
===============
```scala
 "com.stabletechs" %%% "formrx" % "1.1.0"
```
Form.rx is only compiled for Scala.js 0.6+

Quick Demo
==========
[Demo](https://voltir.github.io/form.rx-demo)

[Demo Source](https://github.com/Voltir/form.rx-demo/blob/master/src/main/scala/example/ScalaJSExample.scala)

Using Form.rx
=============
In general two imports are required `import formrx._` and `import formrx.implicits.all._`.

Form.rx Basics
--------------
Form.rx is a macro based tool, built on top of `scala.rx`, that binds a description of a user interface (referred to as a `Layout`) to an instance of a `case class`. 
```scala
//Target case class
case class UserPass(firstname: String, pass : String)

//HTML Form representation
class UserPassLayout(implicit ctx: Ctx.Owner) {
  val firstname = input(`type`:="text").render
  val pass = input(`type`:="password").render
}

//Concrete form instance
val loginForm = FormRx[UserPass,UserPassLayout]
```
The `FormRx` macro is used to construct the two-way data binding between the `UserPass` case class and its `UserPassLayout` form representation, which in this case is two HTML input fields.

This mapping is typesafe and refactor friendly. For instance, if in `UserPass` we rename `firstname` to just `name`, we get the following compile time error:
```scala
[error] /home/nick/tests/formidable-demo/src/main/scala/example/ScalaJSExample.scala:23: The layout is not fully defined: Missing fields are:
[error] --- name
[error]   val form = FormRx[UserPass,UserPassLayout]
[error]                    ^
[error] one error found
[error] (compile:compile) Compilation failed
```
With the loginForm created, it can be used, for example, with scalatags in the following way:
```scala
 import scalatags.JsDom.all._
 val loginTag: HtmlTag = 
   form(
     loginForm.firstname,
     loginForm.pass
   )(
     input(`type`:="Submit"),
     onsubmit := {() =>
     loginForm.current.now match {
       case Success(usrPass) => println(s"$usrPass")
       case Failure(err) => println(s"FAILED!: ${err.getMessage}")
     }
     false
   }
 )
```
In this example, `loginForm.firstname` and `loginForm.pass` are the HTML input elements as defined in `UserPassLayout`. `loginForm.current` has type `Rx[Try[UserPass]]` and is updated every time either input field is modified. In this case though, we are ignoring `scala.rx`s data binding functionality and using `.now` to get out only the latest value when the user hits `Submit`. The resulting `loginTag` is just a normal HtmlTag and can be used in any `scalatag` based project.  

In addition to `.current`, FormRx also provides by default a `.set(...)` and `.reset()` functions. `.set` takes an instance of the target `case class` and populates the form with the data from that case class, for example:
```scala
loginForm.set(UserPass("Bob","secretpass"))
```

`.reset()` on the other hand takes no arguments and simply resets the form to a default state, in this case the empty string is used as the default value and the form is effectively cleared:
```scala
loginForm.reset()
```
The combination of `.set` and `.reset` simplifies the task of "editing" existing data, which makes `FormRx` forms ideal for being used for "upsert" like tasks when form data must both be created and edited.
 
Form.rx Nesting
---------------
Form.rx allows for mapping tree like ADT structures by allowing layouts to nest inside of each other.
For example:
```scala
  case class Inner(foo: String, bar: Int)
  
  case class Nested(top: String, inner: Inner, other: Inner)
```  
To create a `Layout` for `Nested` we can start with `Layout` for `Inner`:
```scala
  class InnerLayout(implicit ctx: Ctx.Owner) {
    val foo = input(`type`:="text").render
    val bar = SelectionRx[Int]()(
      Opt(1)(value:="One","One"),
      Opt(2)(value:="Two","twwo"),
      Opt(42)(value:="Life","What is?"),
      Opt(5)(value:="Five","5ive")
    )
  }
```
Here `Inner.foo` is bound to a simple HTML input element and `Inner.bar` is bound to a HTML selection drop down with 4 possible values.

We can then use `InnerLayout` to define `NestedLayout`:
```scala
  class NestedLayout(implicit ctx: Ctx.Owner) {
    val top = input(`type`:="text").render
    val inner = FormRx[Inner,InnerLayout]
    val other = FormRx[Inner,InnerLayout]
  }
```
Which can then be used, for example, in the following way:
```scala
val nestedForm = FormRx[Nested,NestedLayout]
...
form(
  nestedForm.top,
  label("Inner:"),
  nestedForm.inner.foo,
  nestedForm.inner.bar.select,
  label("Other:"),
  nestedForm.other.foo,
  nestedForm.other.bar.select
)
...
```

Using rx.Vars
--------------
In addition to HTML input elements, `rx.Var`s can be used as a mechanism to bind specific fields, allowing for a great deal of flexibility in building a user interface.

For example, `InnerLayout` could be redefined to the following:
```scala
  class InnerLayout(implicit ctx: Ctx.Owner) {
    val foo = input(`type`:="text").render
    val bar = Var(0)
  }
```
Where `bar` could then be manipulated and used like any other `rx.Var[Int]`, for example (from the demo):
```scala
def buttons(inp: Var[Int]): Rx[HtmlTag] = Rx {
  div(
    label("Current Value: " + inp()),
    ul(cls:="button-group")(
      li(a(cls:="button", onclick:={ () => inp() = inp.now + 1 })("Inc")),
      li(a(cls:="button", onclick:={ () => inp() = inp.now - 1 })("Dec"))
    )
  )
}
...
  buttons(nestedForm.inner.bar)
...
  buttons(nestedForm.other.bar)
...
```

Lists and Sets
--------------
One problem when building a user interface is deciding how to represent data structures like Lists and Sets. 
When building a user interface, there are a myriad of ways one might want to use to present the data, all of which are equally valid. Form.rx includes some utilities to help with common data structures such as Lists and Sets, and is also highly extensible such that other data structures and/or custom layouts can be freely mixed in.

`InputRx.list(...)` and `InputRx.set(...)` defines such a helper for fields that can be considered "tag like" (see example 5 in the demo).

For example, say we want to define a form for the following data:
```scala
  sealed trait SkillLevel
  case object Average extends SkillLevel
  case object Intermediate extends SkillLevel
  case object Expert extends SkillLevel

  case class Skill(name: String, level: SkillLevel)

  case class Profile(foo: String, bar: Int, skills: List[Skill])
```
`InputRx.list(...)` could be used to define a form element for the `skills` field:
```scala
  
  class SkillLayout(implicit ctx: Ctx.Owner) {
    val name = input(`type`:="text").render
    val level = SelectionRx[SkillLevel]()(
      Opt(Average)("Average"),
      Opt(Intermediate)("Intermediate"),
      Opt(Expert)("Expert")
    )
  }

  class ProfileLayout(implicit ctx: Ctx.Owner) {
    val foo = input(`type`:="text").render
    val bar = Var(-1)
    
    def newSkill(txt: String): Skill = Skill(txt,Average)

    val skills = InputRx
      .list(input(`type`:="text", placeholder:="New Skill*"))(newSkill)(() => FormRx[Skill,SkillLayout])
  }
```    
At the heart of `InputRx.list(..)` and `InputRx.set(..)` is some constructor of type `String => T`. The idea is to construct new instances of type `T` given whatever the user types in. However, these helpers also take a constructor for a `FormRx[T]`, allowing each constructed `T` to also be individually edited with some arbitrary form of its own.

Custom FormRx Definitions
-------------------------

TODO
   
   
