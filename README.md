Form.rx
=======
API to map case classes to/from a 'layout' of HTMLInput elements and Scala.Rx Vars. It is akin in purpose to the playframework form library, but entirely done in Scala.js... and a rather different approach in API. 

Contents
========

- [Getting Started](#getting-started)
- [Quick Demo](#quick-demo)
- [Using Form.rx](#using-formrx)
  - [Form.rx Basics](#formrx-basics)

Getting Started
===============

```scala
 "com.stabletechs" %%% "formrx" % "1.1.0"
```
Form.rx is only compiled for Scala.js 0.6+

Quick Demo
==========
[Demo](https://voltir.github.io/formidable-demo)

[Demo Source](https://github.com/Voltir/formidable-demo/blob/master/src/main/scala/example/ScalaJSExample.scala)

Using Form.rx
=============
In general two imports are required `import formrx._` and `import formrx.implicits.all._`.

Form.rx Basics
--------------

Form.rx is a macro based tool, built on top of `scala.rx`, to bind a description of a user interface (referred to as a `Layout`) to an instance of a `case class`. 

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