import sbt._
import Keys._
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._

object Build extends sbt.Build {


  val cross = crossProject.in(file(".")).settings(
    organization := "com.stabletech",
    version := "0.0.3-SNAPSHOT",
    scalaVersion := "2.11.5",
    name := "formidable",
    autoCompilerPlugins := true,
    addCompilerPlugin("com.lihaoyi" %% "acyclic" % "0.1.2"),
    //ScalaJSKeys.postLinkJSEnv := new scala.scalajs.sbtplugin.env.nodejs.NodeJSEnv("nodejs"),
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "scalarx" % Versions.scalarx,
      "com.lihaoyi" %%% "scalatags" % Versions.scalatags,
      "org.scala-lang" % "scala-reflect" % scalaVersion.value 
    ),
    resolvers += "reactivesecurity github repo" at "http://voltir.github.io/formidable/",
    publishTo := Some(Resolver.file(
      "Github Pages", new File("/home/nick/publish/formidable"))
    ) 
  ).jvmSettings(
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "acyclic" % "0.1.2"
    )
  ).jsSettings(
    libraryDependencies ++= Seq(
      "org.scala-js" %%%! "scalajs-dom" % Versions.scalajsDom
    )
  )

  //lazy val root = cross.root.enablePlugins(ScalaJSPlugin)
  lazy val root = cross.enablePlugins(ScalaJSPlugin)


  lazy val jvm = cross.jvm
  
  lazy val js = cross.js
  
  object Versions {
    val scalajsDom = "0.8.0"
    val scalatags = "0.4.5"
    val scalarx = "0.2.7"
    val utest = "0.3.0"
  }
}
