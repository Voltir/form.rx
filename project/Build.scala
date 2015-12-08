import sbt._
import Keys._
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import xerial.sbt.Sonatype
import xerial.sbt.Sonatype._
import xerial.sbt.Sonatype.autoImport._

object Build extends sbt.Build {

  val commonSettings = Seq(
    version := "0.0.11",
    name := "formidable",
    scalaVersion := "2.11.7",
    organization := "com.stabletechs",
    sonatypeProfileName := "com.stabletechs"
  )

  lazy val root = project
    .in(file("."))
    .settings(commonSettings:_*)
    .settings(
      publish := {},
      publishLocal := {}
    )
    .enablePlugins(Sonatype)
    .aggregate(crossJS, crossJVM)

  lazy val cross = crossProject.in(file("."))
    .enablePlugins(ScalaJSPlugin)
    .settings(commonSettings:_*)
    .settings(
      addCompilerPlugin("com.lihaoyi" %% "acyclic" % "0.1.3"),
      libraryDependencies ++= Seq(
        "com.stabletechs" %%% "likelib" % Versions.likelib,
        "com.lihaoyi" %%% "scalarx" % Versions.scalarx,
        "com.lihaoyi" %%% "scalatags" % Versions.scalatags,
        "com.lihaoyi" %%% "utest" % Versions.utest % "test",
        "org.scala-lang" % "scala-reflect" % scalaVersion.value
      ),
      testFrameworks += new TestFramework("utest.runner.Framework"),
      homepage := Some(url("http://stabletechs.com/")),
      licenses += ("MIT License", url("http://www.opensource.org/licenses/mit-license.php")),
      scmInfo := Some(ScmInfo(
        url("https://github.com/Voltir/formidable"),
        "scm:git:git@github.com/Voltir/formidable.git",
        Some("scm:git:git@github.com/Voltir/formidable.git"))
      ),
      publishMavenStyle := true,
      publishTo := {
        val nexus = "https://oss.sonatype.org/"
        if (isSnapshot.value)
          Some("snapshots" at nexus + "content/repositories/snapshots")
        else
          Some("releases" at nexus + "service/local/staging/deploy/maven2")
      },
      pomExtra :=
        <developers>
          <developer>
            <id>Voltaire</id>
            <name>Nick Childers</name>
            <url>https://github.com/voltir/</url>
          </developer>
        </developers>
      ,
      pomIncludeRepository := { _ => false }
    ).jvmSettings(
      libraryDependencies ++= Seq(
        "com.lihaoyi" %% "acyclic" % "0.1.2"
      )
    ).jsSettings(
      preLinkJSEnv := PhantomJSEnv().value,
      libraryDependencies ++= Seq(
        "org.scala-js" %%% "scalajs-dom" % Versions.scalajsDom
      )
    )

  lazy val crossJVM = cross.jvm
  lazy val crossJS = cross.js

  object Versions {
    val scalajsDom = "0.8.2"
    val scalatags = "0.5.2"
    val scalarx = "0.2.8"
    val utest = "0.3.1"
    val likelib = "0.1.1"
  }
}
