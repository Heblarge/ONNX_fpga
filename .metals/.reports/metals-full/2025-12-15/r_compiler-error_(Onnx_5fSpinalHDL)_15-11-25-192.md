error id: 7F401511BCDF33D84ABC6D8C837724D4
file://<WORKSPACE>/hw/spinal/DataPump/BRAMPORT.scala
### java.util.NoSuchElementException: head of empty String

occurred in the presentation compiler.



action parameters:
offset: 317
uri: file://<WORKSPACE>/hw/spinal/DataPump/BRAMPORT.scala
text:
```scala
package DataPump
import spinal.lib._
import spinal.core
import spinal.core._

/**
 * 定义内存读取端口的类型
 * @param AddressWidth 地址总线宽度
 * @param DataWidth 数据总线宽度
 * 这个类继承了Bundle和IMasterSlave，用于定义内存读取端口的接口
 */
case class BRAMReadPort_TypeDef(AddressWidth:Int,DataWidth:Int) extends Bundle with IMasterSlave{

  val Valid=Bits(@@dataWidth/8 bits)  // 标识读取操作是否有效的信号
  val Address=UInt(AddressWidth bits)  // 内存地址信号，宽度由AddressWidth参数决定
  val Data = Bits(DataWidth bits)  // 读取的数据信号，宽度由DataWidth参数决定
  
  /**
   * 配置端口为Master角色
   * 这里将Valid和Address设置为输出，Data设置为输入
   */
  override def asMaster(): Unit ={
    out(Valid,Address)
    in(Data)
  }
  
  /**
   * 配置端口为Slave角色
   * 这里将Valid和Address设置为输入，Data设置为输出
   */
  override def asSlave(): Unit = {
    in(Valid,Address)
    out(Data)
  }
}

/**
 * 定义内存写入端口的类型
 * @param AddressWidth 地址总线宽度
 * @param DataWidth 数据总线宽度
 * 这个类继承了Bundle和IMasterSlave，用于定义内存写入端口的接口
 */
case class BRAMWritePort_TypeDef(AddressWidth:Int,DataWidth:Int) extends Bundle with IMasterSlave{

  val Valid=in Bool()  // 标识写入操作是否有效的信号，作为输入
  val Address=in UInt(AddressWidth bits)  // 内存地址信号，宽度由AddressWidth参数决定，作为输入
  val Data = in(Bits(DataWidth bits))  // 写入的数据信号，宽度由DataWidth参数决定，作为输入
  
  /**
   * 配置端口为Master角色
   * 这里将Valid、Address和Data都设置为输出
   */
  override def asMaster(): Unit ={
    out(Valid,Address,Data)
  }
  
  /**
   * 配置端口为Slave角色
   * 这里将Valid、Address和Data都设置为输入
   */
  override def asSlave(): Unit = {
    in(Valid,Address,Data)
  }
}

```


presentation compiler configuration:
Scala version: 2.13.14
Classpath:
<WORKSPACE>/.bloop/Onnx_SpinalHDL/bloop-bsp-clients-classes/classes-Metals-llefuqC0SVqCNSwvdGPpRg== [exists ], <HOME>/.cache/bloop/semanticdb/com.sourcegraph.semanticdb-javac.0.11.1/semanticdb-javac-0.11.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-library/2.13.14/scala-library-2.13.14.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/github/spinalhdl/spinalhdl-core_2.13/1.11.0/spinalhdl-core_2.13-1.11.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/github/spinalhdl/spinalhdl-lib_2.13/1.11.0/spinalhdl-lib_2.13-1.11.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/github/spinalhdl/spinalhdl-sim_2.13/1.11.0/spinalhdl-sim_2.13-1.11.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/microsoft/onnxruntime/onnxruntime/1.21.0/onnxruntime-1.21.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scalanlp/breeze-viz_2.13/2.1.0/breeze-viz_2.13-2.1.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scalatest/scalatest_2.13/3.2.18/scalatest_2.13-3.2.18.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/github/alexarchambault/case-app_2.13/2.0.6/case-app_2.13-2.0.6.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/github/sbt/junit-interface/0.13.1/junit-interface-0.13.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/apache/commons/commons-lang3/3.14.0/commons-lang3-3.14.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/apache/commons/commons-configuration2/2.11.0/commons-configuration2-2.11.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/google/protobuf/protobuf-java/3.19.4/protobuf-java-3.19.4.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/google/guava/guava/30.1-jre/guava-30.1-jre.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/javax/annotation/javax.annotation-api/1.3.2/javax.annotation-api-1.3.2.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/javax/servlet/javax.servlet-api/4.0.1/javax.servlet-api-4.0.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/projectlombok/lombok/1.18.30/lombok-1.18.30.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/nd4j/nd4j-api/1.0.0-beta6/nd4j-api-1.0.0-beta6.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/nd4j/nd4j-native/1.0.0-beta6/nd4j-native-1.0.0-beta6.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/nd4j/nd4j-native-platform/1.0.0-beta6/nd4j-native-platform-1.0.0-beta6.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/nd4j/nd4j-cuda-10.2/1.0.0-beta6/nd4j-cuda-10.2-1.0.0-beta6.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/tensorflow/tensorflow/1.15.0/tensorflow-1.15.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/tensorflow/proto/1.15.0/proto-1.15.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/reflections/reflections/0.9.11/reflections-0.9.11.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/github/spinalhdl/spinalhdl-idsl-plugin_2.13/1.11.0/spinalhdl-idsl-plugin_2.13-1.11.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scalactic/scalactic_2.13/3.2.18/scalactic_2.13-3.2.18.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-reflect/2.13.14/scala-reflect-2.13.14.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/github/scopt/scopt_2.13/4.1.0/scopt_2.13-4.1.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/sourcecode_2.13/0.3.0/sourcecode_2.13-0.3.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/commons-io/commons-io/2.11.0/commons-io-2.11.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/net/openhft/affinity/3.23.2/affinity-3.23.2.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/slf4j/slf4j-simple/2.0.5/slf4j-simple-2.0.5.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/github/oshi/oshi-core/6.4.0/oshi-core-6.4.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scalanlp/breeze_2.13/2.1.0/breeze_2.13-2.1.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/jfree/jfreechart/1.5.3/jfreechart-1.5.3.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/apache/xmlgraphics/xmlgraphics-commons/1.3.1/xmlgraphics-commons-1.3.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lowagie/itext/2.1.5/itext-2.1.5.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scalatest/scalatest-core_2.13/3.2.18/scalatest-core_2.13-3.2.18.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scalatest/scalatest-featurespec_2.13/3.2.18/scalatest-featurespec_2.13-3.2.18.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scalatest/scalatest-flatspec_2.13/3.2.18/scalatest-flatspec_2.13-3.2.18.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scalatest/scalatest-freespec_2.13/3.2.18/scalatest-freespec_2.13-3.2.18.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scalatest/scalatest-funsuite_2.13/3.2.18/scalatest-funsuite_2.13-3.2.18.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scalatest/scalatest-funspec_2.13/3.2.18/scalatest-funspec_2.13-3.2.18.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scalatest/scalatest-propspec_2.13/3.2.18/scalatest-propspec_2.13-3.2.18.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scalatest/scalatest-refspec_2.13/3.2.18/scalatest-refspec_2.13-3.2.18.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scalatest/scalatest-wordspec_2.13/3.2.18/scalatest-wordspec_2.13-3.2.18.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scalatest/scalatest-diagrams_2.13/3.2.18/scalatest-diagrams_2.13-3.2.18.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scalatest/scalatest-matchers-core_2.13/3.2.18/scalatest-matchers-core_2.13-3.2.18.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scalatest/scalatest-shouldmatchers_2.13/3.2.18/scalatest-shouldmatchers_2.13-3.2.18.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scalatest/scalatest-mustmatchers_2.13/3.2.18/scalatest-mustmatchers_2.13-3.2.18.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/github/alexarchambault/case-app-annotations_2.13/2.0.6/case-app-annotations_2.13-2.0.6.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/github/alexarchambault/case-app-util_2.13/2.0.6/case-app-util_2.13-2.0.6.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/junit/junit/4.13.1/junit-4.13.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-sbt/test-interface/1.0/test-interface-1.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/apache/commons/commons-text/1.12.0/commons-text-1.12.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/commons-logging/commons-logging/1.3.2/commons-logging-1.3.2.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/google/guava/failureaccess/1.0.1/failureaccess-1.0.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/google/guava/listenablefuture/9999.0-empty-to-avoid-conflict-with-guava/listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/google/code/findbugs/jsr305/3.0.2/jsr305-3.0.2.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/checkerframework/checker-qual/3.5.0/checker-qual-3.5.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/google/errorprone/error_prone_annotations/2.3.4/error_prone_annotations-2.3.4.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/google/j2objc/j2objc-annotations/1.3/j2objc-annotations-1.3.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/jakewharton/byteunits/byteunits/0.9.1/byteunits-0.9.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/apache/commons/commons-math3/3.5/commons-math3-3.5.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/google/flatbuffers/flatbuffers-java/1.10.0/flatbuffers-java-1.10.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/nd4j/protobuf/1.0.0-beta6/protobuf-1.0.0-beta6.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.5/slf4j-api-2.0.5.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/nd4j/jackson/1.0.0-beta6/jackson-1.0.0-beta6.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/commons-net/commons-net/3.1/commons-net-3.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/nd4j/nd4j-buffer/1.0.0-beta6/nd4j-buffer-1.0.0-beta6.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/nd4j/nd4j-context/1.0.0-beta6/nd4j-context-1.0.0-beta6.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/net/ericaro/neoitertools/1.0.0/neoitertools-1.0.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/nd4j/nd4j-native/1.0.0-beta6/nd4j-native-1.0.0-beta6-linux-x86_64.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/bytedeco/javacpp/1.5.2/javacpp-1.5.2.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/bytedeco/openblas/0.3.7-1.5.2/openblas-0.3.7-1.5.2.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/bytedeco/openblas/0.3.7-1.5.2/openblas-0.3.7-1.5.2-linux-x86_64.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/bytedeco/mkl/2019.5-1.5.2/mkl-2019.5-1.5.2.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/bytedeco/mkl/2019.5-1.5.2/mkl-2019.5-1.5.2-linux-x86_64.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/nd4j/nd4j-native-api/1.0.0-beta6/nd4j-native-api-1.0.0-beta6.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/bytedeco/openblas-platform/0.3.7-1.5.2/openblas-platform-0.3.7-1.5.2.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/bytedeco/mkl-platform/2019.5-1.5.2/mkl-platform-2019.5-1.5.2.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/nd4j/nd4j-native/1.0.0-beta6/nd4j-native-1.0.0-beta6-android-arm.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/nd4j/nd4j-native/1.0.0-beta6/nd4j-native-1.0.0-beta6-android-arm64.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/nd4j/nd4j-native/1.0.0-beta6/nd4j-native-1.0.0-beta6-android-x86.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/nd4j/nd4j-native/1.0.0-beta6/nd4j-native-1.0.0-beta6-android-x86_64.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/nd4j/nd4j-native/1.0.0-beta6/nd4j-native-1.0.0-beta6-ios-arm64.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/nd4j/nd4j-native/1.0.0-beta6/nd4j-native-1.0.0-beta6-ios-x86_64.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/nd4j/nd4j-native/1.0.0-beta6/nd4j-native-1.0.0-beta6-macosx-x86_64.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/nd4j/nd4j-native/1.0.0-beta6/nd4j-native-1.0.0-beta6-windows-x86_64.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/nd4j/nd4j-native/1.0.0-beta6/nd4j-native-1.0.0-beta6-linux-ppc64le.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/nd4j/nd4j-native/1.0.0-beta6/nd4j-native-1.0.0-beta6-linux-armhf.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/nd4j/nd4j-cuda-10.2/1.0.0-beta6/nd4j-cuda-10.2-1.0.0-beta6-linux-x86_64.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/bytedeco/cuda/10.2-7.6-1.5.2/cuda-10.2-7.6-1.5.2.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/bytedeco/cuda/10.2-7.6-1.5.2/cuda-10.2-7.6-1.5.2-linux-x86_64.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/tensorflow/libtensorflow/1.15.0/libtensorflow-1.15.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/tensorflow/libtensorflow_jni/1.15.0/libtensorflow_jni-1.15.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/javassist/javassist/3.21.0-GA/javassist-3.21.0-GA.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-compiler/2.13.14/scala-compiler-2.13.14.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/github/spinalhdl/spinalhdl-idsl-payload_2.13/1.11.0/spinalhdl-idsl-payload_2.13-1.11.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/net/java/dev/jna/jna/5.14.0/jna-5.14.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/net/java/dev/jna/jna-platform/5.12.1/jna-platform-5.12.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scalanlp/breeze-macros_2.13/2.1.0/breeze-macros_2.13-2.1.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/dev/ludovic/netlib/blas/3.0.1/blas-3.0.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/dev/ludovic/netlib/lapack/3.0.1/lapack-3.0.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/dev/ludovic/netlib/arpack/3.0.1/arpack-3.0.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/net/sourceforge/f2j/arpack_combined_all/0.1/arpack_combined_all-0.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/net/sf/opencsv/opencsv/2.3/opencsv-2.3.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/github/wendykierp/JTransforms/3.1/JTransforms-3.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/modules/scala-collection-compat_2.13/2.7.0/scala-collection-compat_2.13-2.7.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/typelevel/spire_2.13/0.18.0/spire_2.13-0.18.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/bouncycastle/bcmail-jdk14/138/bcmail-jdk14-138.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/bouncycastle/bcprov-jdk14/138/bcprov-jdk14-138.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scalatest/scalatest-compatible/3.2.18/scalatest-compatible-3.2.18.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/modules/scala-xml_2.13/2.1.0/scala-xml_2.13-2.1.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/chuusai/shapeless_2.13/2.3.3/shapeless_2.13-2.3.3.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/nd4j/nd4j-common/1.0.0-beta6/nd4j-common-1.0.0-beta6.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/bytedeco/openblas/0.3.7-1.5.2/openblas-0.3.7-1.5.2-android-arm.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/bytedeco/openblas/0.3.7-1.5.2/openblas-0.3.7-1.5.2-android-arm64.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/bytedeco/openblas/0.3.7-1.5.2/openblas-0.3.7-1.5.2-android-x86.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/bytedeco/openblas/0.3.7-1.5.2/openblas-0.3.7-1.5.2-android-x86_64.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/bytedeco/openblas/0.3.7-1.5.2/openblas-0.3.7-1.5.2-ios-arm64.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/bytedeco/openblas/0.3.7-1.5.2/openblas-0.3.7-1.5.2-ios-x86_64.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/bytedeco/openblas/0.3.7-1.5.2/openblas-0.3.7-1.5.2-linux-x86.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/bytedeco/openblas/0.3.7-1.5.2/openblas-0.3.7-1.5.2-linux-armhf.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/bytedeco/openblas/0.3.7-1.5.2/openblas-0.3.7-1.5.2-linux-arm64.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/bytedeco/openblas/0.3.7-1.5.2/openblas-0.3.7-1.5.2-linux-ppc64le.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/bytedeco/openblas/0.3.7-1.5.2/openblas-0.3.7-1.5.2-macosx-x86_64.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/bytedeco/openblas/0.3.7-1.5.2/openblas-0.3.7-1.5.2-windows-x86.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/bytedeco/openblas/0.3.7-1.5.2/openblas-0.3.7-1.5.2-windows-x86_64.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/bytedeco/mkl/2019.5-1.5.2/mkl-2019.5-1.5.2-linux-x86.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/bytedeco/mkl/2019.5-1.5.2/mkl-2019.5-1.5.2-macosx-x86_64.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/bytedeco/mkl/2019.5-1.5.2/mkl-2019.5-1.5.2-windows-x86.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/bytedeco/mkl/2019.5-1.5.2/mkl-2019.5-1.5.2-windows-x86_64.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/io/github/java-diff-utils/java-diff-utils/4.12/java-diff-utils-4.12.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/jline/jline/3.25.1/jline-3.25.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/pl/edu/icm/JLargeArrays/1.5/JLargeArrays-1.5.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/typelevel/spire-macros_2.13/0.18.0/spire-macros_2.13-0.18.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/typelevel/spire-platform_2.13/0.18.0/spire-platform_2.13-0.18.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/typelevel/spire-util_2.13/0.18.0/spire-util_2.13-0.18.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/typelevel/algebra_2.13/2.8.0/algebra_2.13-2.8.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/nd4j/guava/1.0.0-beta6/guava-1.0.0-beta6.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/apache/commons/commons-compress/1.18/commons-compress-1.18.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/commons-codec/commons-codec/1.10/commons-codec-1.10.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/typelevel/cats-kernel_2.13/2.8.0/cats-kernel_2.13-2.8.0.jar [exists ]
Options:
-language:postfixOps -Yrangepos -Xplugin-require:semanticdb




#### Error stacktrace:

```
scala.collection.StringOps$.head$extension(StringOps.scala:1124)
	scala.meta.internal.metals.ClassfileComparator.compare(ClassfileComparator.scala:30)
	scala.meta.internal.metals.ClassfileComparator.compare(ClassfileComparator.scala:3)
	java.base/java.util.PriorityQueue.siftUpUsingComparator(PriorityQueue.java:660)
	java.base/java.util.PriorityQueue.siftUp(PriorityQueue.java:637)
	java.base/java.util.PriorityQueue.offer(PriorityQueue.java:330)
	java.base/java.util.PriorityQueue.add(PriorityQueue.java:311)
	scala.meta.internal.metals.ClasspathSearch.$anonfun$search$3(ClasspathSearch.scala:32)
	scala.meta.internal.metals.ClasspathSearch.$anonfun$search$3$adapted(ClasspathSearch.scala:26)
	scala.collection.IterableOnceOps.foreach(IterableOnce.scala:630)
	scala.collection.IterableOnceOps.foreach$(IterableOnce.scala:628)
	scala.collection.AbstractIterator.foreach(Iterator.scala:1313)
	scala.meta.internal.metals.ClasspathSearch.search(ClasspathSearch.scala:26)
	scala.meta.internal.metals.WorkspaceSymbolProvider.search(WorkspaceSymbolProvider.scala:107)
	scala.meta.internal.metals.MetalsSymbolSearch.search$1(MetalsSymbolSearch.scala:114)
	scala.meta.internal.metals.MetalsSymbolSearch.search(MetalsSymbolSearch.scala:118)
	scala.meta.internal.pc.AutoImportsProvider.autoImports(AutoImportsProvider.scala:48)
	scala.meta.internal.pc.ScalaPresentationCompiler.$anonfun$autoImports$1(ScalaPresentationCompiler.scala:399)
	scala.meta.internal.pc.CompilerAccess.withSharedCompiler(CompilerAccess.scala:148)
	scala.meta.internal.pc.CompilerAccess.$anonfun$withInterruptableCompiler$1(CompilerAccess.scala:92)
	scala.meta.internal.pc.CompilerAccess.$anonfun$onCompilerJobQueue$1(CompilerAccess.scala:209)
	scala.meta.internal.pc.CompilerJobQueue$Job.run(CompilerJobQueue.scala:152)
	java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1136)
	java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:635)
	java.base/java.lang.Thread.run(Thread.java:840)
```
#### Short summary: 

java.util.NoSuchElementException: head of empty String