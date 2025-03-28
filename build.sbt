ThisBuild / version := "1.0"
ThisBuild / scalaVersion := "2.13.14"
ThisBuild / organization := "org.example"

val spinalVersion = "1.11.0"
val spinalCore = "com.github.spinalhdl" %% "spinalhdl-core" % spinalVersion
val spinalLib = "com.github.spinalhdl" %% "spinalhdl-lib" % spinalVersion
val spinalIdslPlugin = compilerPlugin("com.github.spinalhdl" %% "spinalhdl-idsl-plugin" % spinalVersion)
val nd4jVersion = "1.0.0-beta6" // 请根据需要调整版本号
val tensorflowVersion = "1.15.0" // 你可以根据需要调整版本号

lazy val projectname = (project in file("."))
  .settings(
    Compile / scalaSource := baseDirectory.value / "hw" / "spinal",
    //scalacOptions ++= Seq("-encoding", "UTF-8"),//用来支持中文注释
    libraryDependencies ++= Seq(spinalCore, spinalLib, spinalIdslPlugin),
    Compile / javaSource := baseDirectory.value / "sw"/"java"/"main",
    Test / javaSource := baseDirectory.value / "sw"/"java"/"test",
    javacOptions ++= Seq("-encoding", "UTF-8"),//用来支持中文注释
    //onnxruntime 依赖
    libraryDependencies += "com.microsoft.onnxruntime" % "onnxruntime" % "1.21.0",

    

    //onnx4j
    libraryDependencies += "com.github.sbt" % "junit-interface" % "0.13.1",
    libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.12.0",
    libraryDependencies += "org.apache.commons" % "commons-configuration2" % "2.11.0",
    libraryDependencies += "com.google.protobuf" % "protobuf-java" % "3.19.4",
    libraryDependencies += "com.google.guava" % "guava" % "30.1-jre",
    libraryDependencies += "javax.annotation" % "javax.annotation-api" % "1.3.2",
    libraryDependencies += "javax.servlet" % "javax.servlet-api" % "4.0.1",
    libraryDependencies ++= Seq(
      "org.nd4j" % "nd4j-api" % nd4jVersion,
      "org.nd4j" % "nd4j-native" % nd4jVersion,
      "org.nd4j" % "nd4j-native-platform" % nd4jVersion,
      "org.nd4j" % "nd4j-cuda-10.2" % nd4jVersion
    ),
    libraryDependencies ++= Seq(
      "org.tensorflow" % "tensorflow" % tensorflowVersion,
      "org.tensorflow" % "proto" % tensorflowVersion exclude("com.google.protobuf", "protobuf-java")
    ),
    // https://mvnrepository.com/artifact/org.reflections/reflections
libraryDependencies += "org.reflections" % "reflections" % "0.9.11"
  )
// build.sbt
crossPaths := false
fork := true
