import sbt._
import Keys._
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._

object Build extends sbt.Build {


  val cross = new utest.jsrunner.JsCrossBuild(
    organization := "com.stabletech",
    version := "0.0.2-SNAPSHOT",
    scalaVersion := "2.11.4",
    name := "formidable",
    autoCompilerPlugins := true,
    addCompilerPlugin("com.lihaoyi" %% "acyclic" % "0.1.2"),
    //ScalaJSKeys.postLinkJSEnv := new scala.scalajs.sbtplugin.env.nodejs.NodeJSEnv("nodejs"),
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "acyclic" % "0.1.2",
      "org.scala-lang" % "scala-reflect" % scalaVersion.value 
    )
  )

  lazy val root = cross.root.enablePlugins(ScalaJSPlugin)

  lazy val js = cross.js.settings(
    //ScalaJSKeys.jsDependencies += scala.scalajs.sbtplugin.RuntimeDOM,
    libraryDependencies ++= Seq(
      "org.scala-js" %%%! "scalajs-dom" % Versions.scalajsDom,
      //"com.lihaoyi" %%% "utest" % Versions.utest,
      "com.lihaoyi" %%%! "scalarx" % Versions.scalarx,
      "com.lihaoyi" %%%! "scalatags" % Versions.scalatags
    )
  )

  lazy val jvm = cross.jvm.settings(
    libraryDependencies ++= Seq(
      //"com.lihaoyi" %% "utest" % Versions.utest,
      "com.lihaoyi" %% "scalarx" % Versions.scalarx,
      "com.lihaoyi" %% "scalatags" % Versions.scalatags
    )
  )

  object Versions {
    val scalajsDom = "0.7.0"
    val scalatags = "0.4.3-RC1"
    val scalarx = "0.2.7-RC1"
    val utest = "0.2.5-RC1"
  }
}
