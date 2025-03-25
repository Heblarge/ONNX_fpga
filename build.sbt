ThisBuild / version := "1.0"
ThisBuild / scalaVersion := "2.13.14"
ThisBuild / organization := "org.example"

val spinalVersion = "1.11.0"
val spinalCore = "com.github.spinalhdl" %% "spinalhdl-core" % spinalVersion
val spinalLib = "com.github.spinalhdl" %% "spinalhdl-lib" % spinalVersion
val spinalIdslPlugin = compilerPlugin("com.github.spinalhdl" %% "spinalhdl-idsl-plugin" % spinalVersion)

lazy val projectname = (project in file("."))
  .settings(
    Compile / scalaSource := baseDirectory.value / "hw" / "spinal",
    scalacOptions += "-encoding=UTF-8",//用来支持中文注释
    libraryDependencies ++= Seq(spinalCore, spinalLib, spinalIdslPlugin),
    Compile / javaSource := baseDirectory.value / "hw"/"java",
    javacOptions ++= Seq("-encoding", "UTF-8"),//用来支持中文注释
    libraryDependencies += "com.microsoft.onnxruntime" % "onnxruntime" % "1.21.0",
    libraryDependencies ++= Seq(
      "org.junit.jupiter" % "junit-jupiter-api" % "5.7.0",
      "org.junit.jupiter" % "junit-jupiter-engine" % "5.7.0"
    ),
  )

fork := true
