
val formrx = crossProject.settings(
  version := "1.1.0",
  name := "formrx",
  scalaVersion := "2.11.7",
  organization := "com.stabletechs",
  testFrameworks += new TestFramework("utest.runner.Framework"),
  addCompilerPlugin("com.lihaoyi" %% "acyclic" % "0.1.3"),
  libraryDependencies ++= Seq(
    "com.stabletechs" %%% "likelib" % "0.1.1",
    "com.lihaoyi" %%% "scalarx" % "0.3.1",
    "com.lihaoyi" %%% "scalatags" % "0.5.4",
    "com.lihaoyi" %%% "utest" % "0.3.1" % "test",
    "org.scala-lang" % "scala-reflect" % scalaVersion.value
  ),
  scmInfo := Some(ScmInfo(
    url("https://github.com/Voltir/form,rx"),
    "scm:git:git@github.com/Voltir/form.rx.git",
    Some("scm:git:git@github.com/Voltir/form.rx.git"))
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
  homepage := Some(url("http://stabletechs.com/")),
  licenses += ("MIT License", url("http://www.opensource.org/licenses/mit-license.php")),
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
).jsSettings(
  jsDependencies += RuntimeDOM % "test",
  preLinkJSEnv := PhantomJSEnv().value,
  scalaJSStage in Test := FullOptStage,
  scalaJSUseRhino in Global := false,
  libraryDependencies ++= Seq(
    "org.scala-js" %%% "scalajs-dom" % "0.9.0"
  )
)

lazy val formrxJVM = formrx.jvm

lazy val formrxJS = formrx.js
