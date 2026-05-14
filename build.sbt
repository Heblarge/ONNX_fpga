ThisBuild / version := "1.0"
ThisBuild / scalaVersion := "2.13.14"
ThisBuild / organization := "org.example"

val nd4jVersion = "1.0.0-beta6"
val tensorflowVersion = "1.15.0"

// ==========================================
// 1. 主源码目录（只编译 Java，Scala 指向 dummy）
// ==========================================
Compile / scalaSource := baseDirectory.value / "src_dummy" / "scala"
Compile / javaSource := baseDirectory.value / "sw" / "java" / "main" / "java"
Compile / resourceDirectory := baseDirectory.value / "sw" / "java" / "main" / "resources"

// ==========================================
// 2. 保留测试路径（你将在这里写依赖加载测试类）
// ==========================================
Test / scalaSource := baseDirectory.value / "sw" / "scala" / "test" / "scala"
Test / javaSource := baseDirectory.value / "sw" / "java" / "test" / "java"
Test / resourceDirectory := baseDirectory.value / "sw" / "java" / "test" / "resources"

// ==========================================
// 3. 编译与运行参数（极度重要，缺一不可）
// ==========================================
fork := true
crossPaths := false
javacOptions ++= Seq("-encoding", "UTF-8", "-Xlint:unchecked", "-Xlint:deprecation", "-parameters")
Compile / doc / sources := Seq.empty

// ND4J 和 TF 的老版本在高版本 JDK 上加载 .so 必须的反射放行参数
val commonJavaOptions = Seq(
  "--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED",
  "--add-exports=java.base/jdk.internal.ref=ALL-UNNAMED",
  "--add-opens=java.base/jdk.internal.ref=ALL-UNNAMED",
  "--add-exports=java.base/sun.nio.ch=ALL-UNNAMED",
  "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
  "--add-exports=java.base/sun.nio=ALL-UNNAMED",
  "--add-opens=java.base/java.nio=ALL-UNNAMED"
)

// 设置 JNI 库路径（从环境变量读取，或使用默认值）
val jniLibPath = sys.env.get("JNI_LIB_PATH")
  .orElse(sys.props.get("jni.lib.path"))
  .orElse(Some("/run/media/nvme0n1/accelerator/shared_object")) // 板子上的默认路径

val jniOption = jniLibPath.map(p => s"-Djava.library.path=$p").toSeq

// 应用到 Compile 和 Test 配置
Compile / javaOptions := commonJavaOptions ++ jniOption
Test / javaOptions := commonJavaOptions ++ jniOption

// ==========================================
// 4. 纯净的混合推理依赖树（已剔除 SpinalHDL 和 CUDA）
// ==========================================
javacOptions ++= Seq("-encoding", "UTF-8")//用来支持中文注释

libraryDependencies ++= Seq(
  // --- 模型解析层 ---
  "com.microsoft.onnxruntime" % "onnxruntime" % "1.21.0",
  "com.google.protobuf" % "protobuf-java" % "3.19.4",

  // --- HWAccelerated 后端需要 ND4J API 和 Native Backend ---
  "org.nd4j" % "nd4j-native-platform" % nd4jVersion,

  "org.tensorflow" % "tensorflow" % tensorflowVersion,
  "org.tensorflow" % "proto" % tensorflowVersion exclude("com.google.protobuf", "protobuf-java"),

  // --- 基础工具依赖 ---
  "org.apache.commons" % "commons-lang3" % "3.12.0",
  "org.apache.commons" % "commons-configuration2" % "2.11.0",
  "com.google.guava" % "guava" % "30.1-jre",
  "org.reflections" % "reflections" % "0.9.11",
  "javax.annotation" % "javax.annotation-api" % "1.3.2",
  "javax.servlet" % "javax.servlet-api" % "4.0.1",
  "org.projectlombok" % "lombok" % "1.18.30" % Provided,
  "com.github.alexarchambault" %% "case-app" % "2.0.6",

  // --- 测试框架 ---
  "org.scalatest" %% "scalatest" % "3.2.18" % Test,
  "com.github.sbt" % "junit-interface" % "0.13.1" % Test
)

// ==========================================
// 5. Assembly 配置（创建可执行 fat jar）
// ==========================================
assembly / assemblyJarName := "hw-accelerated-test.jar"

assembly / assemblyMergeStrategy := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

assembly / mainClass := Some("org.forwarder.demo.DL4JTest")

// 在 Test 配置中也启用 assembly
Test / assembly / assemblyJarName := "hw-accelerated-test.jar"
Test / assembly / assemblyMergeStrategy := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}
Test / assembly / mainClass := Some("org.forwarder.demo.DL4JTest")

// 让 assembly 包含测试类
Test / assembly / fullClasspath := (Test / fullClasspath).value
