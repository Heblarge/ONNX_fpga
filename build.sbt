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
    Compile / javaSource := baseDirectory.value / "sw"/"java",
    javacOptions ++= Seq("-encoding", "UTF-8"),//用来支持中文注释
    //onnxruntime 依赖
    libraryDependencies += "com.microsoft.onnxruntime" % "onnxruntime" % "1.21.0",
    
    //libraryDependencies ++= Seq(
    //  "org.junit.jupiter" % "junit-jupiter-api" % "5.7.0" % Test,
    //  "org.junit.jupiter" % "junit-jupiter-engine" % "5.7.0" % Test,
//
    //),
    // https://mvnrepository.com/artifact/junit/junit
    //libraryDependencies += "junit" % "junit" % "4.13.1" % Test,
    libraryDependencies += "com.github.sbt" % "junit-interface" % "0.13.1",
    //libraryDependencies += "org.hamcrest" % "hamcrest-core" % "1.3" % Test,
    //libraryDependencies += "org.hamcrest" % "hamcrest-library" % "1.3" % Test,
    //onnx4j
    //libraryDependencies += "org" %% "forwarder.backend.tensorflow" % "0.0.1-SNAPSHOT",
    //libraryDependencies += "org" %% "forwarder.backend.dl4j" % "0.0.1",
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
    //libraryDependencies ++= Seq(
    //  "org.springframework.boot" % "spring-boot-starter" % "2.1.6.RELEASE",
//
    //  "org.springframework.boot" % "spring-boot-starter-web" % "2.1.6.RELEASE",
    //  "org.springframework.boot" % "spring-boot-devtools" % "2.1.6.RELEASE",
    //  "org.springframework.boot" % "spring-boot-configuration-processor" % "2.1.6.RELEASE",
    //  // https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-starter-test
    //  "org.springframework.boot" % "spring-boot-starter-test" % "2.1.6.RELEASE" % Test
    //),
    //libraryDependencies += "org.projectlombok" % "lombok"% "1.18.10" % Provided,
    //libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3",
    //libraryDependencies += "com.twelvemonkeys" % "twelvemonkeys-core" % "2.3",
    //libraryDependencies += "com.twelvemonkeys" % "twelvemonkeys-imageio" % "2.3" pomOnly(),
    //libraryDependencies += "com.twelvemonkeys" % "twelvemonkeys" % "3.8.3" pomOnly()
    

  )
// build.sbt
crossPaths := false
fork := true
