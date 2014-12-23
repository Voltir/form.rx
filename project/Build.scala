import sbt._
import Keys._

import scala.scalajs.sbtplugin.ScalaJSPlugin
import scala.scalajs.sbtplugin.ScalaJSPlugin._
import ScalaJSKeys._

object Build extends sbt.Build {


  val cross = new utest.jsrunner.JsCrossBuild(
    organization := "com.stabletech",
    version := "0.0.1",
    scalaVersion := "2.11.4",
    name := "formidable",
    ScalaJSKeys.postLinkJSEnv := new scala.scalajs.sbtplugin.env.nodejs.NodeJSEnv("nodejs"),
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "acyclic" % "0.1.2" % "provided",
      "org.scala-lang" % "scala-reflect" % scalaVersion.value % "provided"
    )
  )

  lazy val root = cross.root

  lazy val js = cross.js.settings(
    ScalaJSKeys.jsDependencies += scala.scalajs.sbtplugin.RuntimeDOM,
    libraryDependencies ++= Seq(
      "org.scala-lang.modules.scalajs" %%% "scalajs-dom" % "0.6",
      "com.lihaoyi" %%% "utest" % Versions.utest,
      "com.scalarx" %%% "scalarx" % Versions.scalarx,
      "com.scalatags" %%% "scalatags" % Versions.scalatags
    )
  )

  lazy val jvm = cross.jvm.settings(
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "utest" % Versions.utest,
      "com.scalarx" %% "scalarx" % Versions.scalarx,
      "com.scalatags" %% "scalatags" % Versions.scalatags
    )
  )

  object Versions {
    val scalatags = "0.4.2"
    val scalarx = "0.2.6"
    val utest = "0.2.4"
  }
}
