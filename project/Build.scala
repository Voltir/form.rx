import sbt._
import Keys._
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._

object Build extends sbt.Build {

  val cross = crossProject.in(file(".")).settings(
    organization := "com.stabletech",
    version := "0.0.8-SNAPSHOT",
    scalaVersion := "2.11.6",
    name := "formidable",
    autoCompilerPlugins := true,
    addCompilerPlugin("com.lihaoyi" %% "acyclic" % "0.1.2"),
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "scalarx" % Versions.scalarx,
      "com.lihaoyi" %%% "scalatags" % Versions.scalatags,
      "com.lihaoyi" %%% "utest" % Versions.utest % "test",
      "org.scala-lang" % "scala-reflect" % scalaVersion.value 
    ),
   testFrameworks += new TestFramework("utest.runner.Framework"),
    publishTo := Some(Resolver.file(
      "Github Pages", new File("/home/nick/publish/formidable"))
    ) 
  ).jvmSettings(
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "acyclic" % "0.1.2"
    ),
    publishTo := Some(Resolver.file(
      "Github Pages", new File("/home/nick/publish/formidable"))
    )
  ).jsSettings(
    preLinkJSEnv := PhantomJSEnv().value,
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % Versions.scalajsDom
    )
  )

  val root = cross.enablePlugins(ScalaJSPlugin)

  val jvm = cross.jvm
  
  val js = cross.js
  
  object Versions {
    val scalajsDom = "0.8.0"
    val scalatags = "0.5.1"
    val scalarx = "0.2.8"
    val utest = "0.3.1"
  }
}
