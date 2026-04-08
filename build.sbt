ThisBuild / version := "1.0"
ThisBuild / scalaVersion := "2.13.14"
ThisBuild / organization := "org.example"

val spinalVersion = "1.11.0"
val spinalCore = "com.github.spinalhdl" %% "spinalhdl-core" % spinalVersion
val spinalLib = "com.github.spinalhdl" %% "spinalhdl-lib" % spinalVersion
val spinalIdslPlugin = compilerPlugin("com.github.spinalhdl" %% "spinalhdl-idsl-plugin" % spinalVersion)
val nd4jVersion = "1.0.0-beta6" // 请根据需要调整版本号
val tensorflowVersion = "1.15.0" // 你可以根据需要调整版本号

lazy val Onnx_SpinalHDL = (project in file("."))
  .settings(
    Compile / scalaSource := baseDirectory.value / "hw" / "spinal",
    Compile / unmanagedSourceDirectories += baseDirectory.value / "sw" / "scala" / "main" / "scala",
    Test / unmanagedSourceDirectories += baseDirectory.value / "sw" / "scala" / "test" / "scala",
    Compile / unmanagedResourceDirectories += baseDirectory.value / "sw" / "scala" / "main" / "resources",
    Test / unmanagedResourceDirectories += baseDirectory.value / "sw" / "scala" / "test" / "resources",
    scalacOptions ++= Seq("-language:postfixOps"),
    //scalacOptions ++= Seq("-encoding", "UTF-8"),//用来支持中文注释
    libraryDependencies ++= Seq(spinalCore, spinalLib, spinalIdslPlugin,"com.github.spinalhdl" %% "spinalhdl-sim" % spinalVersion),
    libraryDependencies += "com.microsoft.onnxruntime" % "onnxruntime" % "1.21.0",
    libraryDependencies += "org.scalanlp" %% "breeze-viz" % "2.1.0",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.18",
    libraryDependencies += "com.github.alexarchambault" %% "case-app" % "2.0.6"


  )
lazy val Onnx_SpinalHDL_test = (project in file("."))
.settings(
  name := "onnx4j_test",
)
.dependsOn(Onnx_SpinalHDL)

// build.sbt
//手动设置java的source和resource的位置
javacOptions ++= Seq("-encoding", "UTF-8")//用来支持中文注释
Compile / javaSource := baseDirectory.value / "sw"/ "java"/"main"/"java"
Test / javaSource := baseDirectory.value / "sw" / "java"/"test"/"java"
Compile / resourceDirectory := baseDirectory.value / "sw" /"java"/"main"/ "resources"
Test / resourceDirectory := baseDirectory.value / "sw" /"java"/"test"/ "resources"
javaOptions ++= Seq(
  "--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED",
  "--add-exports=java.base/jdk.internal.ref=ALL-UNNAMED",
  "--add-opens=java.base/jdk.internal.ref=ALL-UNNAMED",
  "--add-exports=java.base/sun.nio.ch=ALL-UNNAMED",
  "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
  "--add-exports=java.base/sun.nio=ALL-UNNAMED",
  "--add-opens=java.base/java.nio=ALL-UNNAMED",
  //! if you are using IDEA with lombok plugin, uncomment the following line:
  //"-processor",
  //"lombok.launch.AnnotationProcessorHider$AnnotationProcessor"
  
)
Compile / compile := (Compile / compile).value
Compile / doc / sources := Seq.empty
Compile / compile / javacOptions ++= Seq(
  "-Xlint:unchecked",
  "-Xlint:deprecation",
  "-parameters"
)

//onnx4j
libraryDependencies += "com.github.sbt" % "junit-interface" % "0.13.1"
libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.12.0"
libraryDependencies += "org.apache.commons" % "commons-configuration2" % "2.11.0"
libraryDependencies += "com.google.protobuf" % "protobuf-java" % "3.19.4"
libraryDependencies += "com.google.guava" % "guava" % "30.1-jre"
libraryDependencies += "javax.annotation" % "javax.annotation-api" % "1.3.2"
libraryDependencies += "javax.servlet" % "javax.servlet-api" % "4.0.1"

libraryDependencies += "org.projectlombok" % "lombok" % "1.18.30" % Provided



libraryDependencies ++= Seq(
  "org.nd4j" % "nd4j-api" % nd4jVersion,
  "org.nd4j" % "nd4j-native" % nd4jVersion,
  "org.nd4j" % "nd4j-native-platform" % nd4jVersion,
  "org.nd4j" % "nd4j-cuda-10.2" % nd4jVersion
)
libraryDependencies ++= Seq(
  "org.tensorflow" % "tensorflow" % tensorflowVersion,
  "org.tensorflow" % "proto" % tensorflowVersion exclude("com.google.protobuf", "protobuf-java")
)
// https://mvnrepository.com/artifact/org.reflections/reflections
libraryDependencies += "org.reflections" % "reflections" % "0.9.11"

// case app
libraryDependencies += "com.github.alexarchambault" %% "case-app" % "2.0.6"

// scalatest
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.18"

crossPaths := false
fork := true
