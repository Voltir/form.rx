import sbt._
import Keys._
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import xerial.sbt.Sonatype
import xerial.sbt.Sonatype._
import xerial.sbt.Sonatype.autoImport._

object Build extends sbt.Build {
 
  val versionV = "0.0.11-SNAPSHOT"
 
  lazy val root = project
    .in(file("."))
    .enablePlugins(ScalaJSPlugin,Sonatype)
    .aggregate(crossJS, crossJVM)
    .settings(
      organization := "com.stabletechs",
      version := versionV,
      name := "formidable",
      sonatypeProfileName := "com.stabletechs",
      scalaVersion := "2.11.7",
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
      sonatypeProfileName := "com.stabletechs",
      pomExtra := (
        <developers>
          <developer>
            <id>Voltaire</id>
            <name>Nick Childers</name>
            <url>https://github.com/voltir/</url>
          </developer>
        </developers>
      ),
      pomIncludeRepository := { _ => false }
    )

  lazy val cross = crossProject.in(file(".")).settings(
    scalaVersion := "2.11.7",
    version := versionV,
    organization := "com.stabletechs",
    name := "formidable",
    homepage := Some(url("http://stabletechs.com/")),
    licenses += ("MIT License", url("http://www.opensource.org/licenses/mit-license.php")),
    autoCompilerPlugins := true,
    addCompilerPlugin("com.lihaoyi" %% "acyclic" % "0.1.2"),
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "scalarx" % Versions.scalarx,
      "com.lihaoyi" %%% "scalatags" % Versions.scalatags,
      "com.lihaoyi" %%% "utest" % Versions.utest % "test",
      "org.scala-lang" % "scala-reflect" % scalaVersion.value 
    ),
    testFrameworks += new TestFramework("utest.runner.Framework"),
    scmInfo := Some(ScmInfo(
      url("https://github.com/Voltir/formidable"),
      "scm:git:git@github.com/Voltir/formidable.git",
      Some("scm:git:git@github.com/Voltir/formidable.git"))
    ),
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
       Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    sonatypeProfileName := "com.stabletechs",
    pomExtra := (
      <developers>
        <developer>
          <id>Voltaire</id>
          <name>Nick Childers</name>
          <url>https://github.com/voltir/</url>
        </developer>
      </developers>
    ),
    pomIncludeRepository := { _ => false } 
  ).jvmSettings(
    scalaVersion := "2.11.7",
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "acyclic" % "0.1.2"
    )
  ).jsSettings(
    scalaVersion := "2.11.7",
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
  }
}
