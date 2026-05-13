error id: file://<WORKSPACE>/sw/java/test/java/org/forwarder/backend/impls/dl4j/opsets/aiOnnx/v13/ops/DL4JCastV13Test.java
file://<WORKSPACE>/sw/java/test/java/org/forwarder/backend/impls/dl4j/opsets/aiOnnx/v13/ops/DL4JCastV13Test.java
### com.thoughtworks.qdox.parser.ParseException: syntax error @[81,4]

error in qdox parser
file content:
```java
offset: 3580
uri: file://<WORKSPACE>/sw/java/test/java/org/forwarder/backend/impls/dl4j/opsets/aiOnnx/v13/ops/DL4JCastV13Test.java
text:
```scala
//package org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v13.ops;
//
//import static org.junit.Assert.assertArrayEquals;
//import static org.junit.Assert.assertEquals;
//
//import org.forwarder.backend.impls.dl4j.DL4JSession;
//import org.forwarder.backend.impls.dl4j.DL4JTestCase;
//import org.junit.Test;
//import org.nd4j.linalg.api.ndarray.INDArray;
//import org.nd4j.linalg.factory.Nd4j;
//import org.onnx4j.tensor.DataType;
//
//public class DL4JCastV13Test extends DL4JTestCase {
//
//    // ------------------------- 合法转换测试 -------------------------
//    @Test
//    public void testFloatToInt() throws Exception {
//        try (
//                INDArray input = Nd4j.create(new float[]{1.8f, -2.3f, 3.0f});
//                INDArray expected = Nd4j.create(new int[]{1, -2, 3}, new long[]{3}, org.nd4j.linalg.api.buffer.DataType.INT)
//        ) {
//            testCast(input, expected, DataType.FLOAT, DataType.INT32);
//        }
//    }
//
//    @Test
//    public void testInt64ToFloat() throws Exception {
//        try (
//                INDArray input = Nd4j.create(new long[]{100L, -200L, 300L}, new long[]{3}, org.nd4j.linalg.api.buffer.DataType.LONG);
//                INDArray expected = Nd4j.create(new float[]{100f, -200f, 300f})
//        ) {
//            testCast(input, expected, DataType.INT64, DataType.FLOAT);
//        }
//    }
//
//    @Test
//    public void testFloatToBool() throws Exception {
//        try (
//                INDArray input = Nd4j.create(new float[]{0f, 1.5f, -0.1f});
//                INDArray expected = Nd4j.create(new boolean[]{false, true, true})
//        ) {
//            testCast(input, expected, DataType.FLOAT, DataType.BOOL);
//        }
//    }
//
//    // ------------------------- 边界值测试 -------------------------
//    @Test
//    public void testInt32ToInt8Overflow() throws Exception {
//        try (
//                INDArray input = Nd4j.create(new int[]{128, -129, 255}, new long[]{3}, org.nd4j.linalg.api.buffer.DataType.INT);
//                INDArray expected = Nd4j.create(new byte[]{-128, 127, -1}, new long[]{3}, org.nd4j.linalg.api.buffer.DataType.BYTE)
//        ) {
//            testCast(input, expected, DataType.INT32, DataType.INT8);
//        }
//    }
//
//    // ------------------------- 测试工具方法 -------------------------
//    private void testCast(INDArray input, INDArray expected, DataType inputType, DataType targetType) throws Exception {
//        try (DL4JSession session = new DL4JSession(null)) {
//            testCastV13 operator = new testCastV13();
//            INDArray output = operator.cast(input, targetType); // 直接传DataType
//
//            assertArrayEquals(
//                    String.format("[%s -> %s] Shape mismatch", inputType, targetType),
//                    expected.shape(), output.shape());
//            assertEquals(
//                    String.format("[%s -> %s] DataType mismatch", inputType, targetType),
//                    expected.dataType(), output.dataType());
//
//            if (!targetType.equals(DataType.FLOAT16)) {
//                assertEquals(
//                        String.format("[%s -> %s] Data content mismatch", inputType, targetType),
//                        expected.toString(), output.toString());
//            }
//            System.out.printf("Test [%s -> %s] PASSED%n", inputType, targetType);
//        } catch (Throwable t) {
//            System.err.printf("Test [%s -> %s] FAILED: %s%n", inputType, targetType, t.getMessage());
//            throw t;  // 重新抛出异常，保证JUnit能识别测试失败
//        }
//    }
//}@@
```

```



#### Error stacktrace:

```
com.thoughtworks.qdox.parser.impl.Parser.yyerror(Parser.java:2025)
	com.thoughtworks.qdox.parser.impl.Parser.yyparse(Parser.java:2147)
	com.thoughtworks.qdox.parser.impl.Parser.parse(Parser.java:2006)
	com.thoughtworks.qdox.library.SourceLibrary.parse(SourceLibrary.java:232)
	com.thoughtworks.qdox.library.SourceLibrary.parse(SourceLibrary.java:190)
	com.thoughtworks.qdox.library.SourceLibrary.addSource(SourceLibrary.java:94)
	com.thoughtworks.qdox.library.SourceLibrary.addSource(SourceLibrary.java:89)
	com.thoughtworks.qdox.library.SortedClassLibraryBuilder.addSource(SortedClassLibraryBuilder.java:162)
	com.thoughtworks.qdox.JavaProjectBuilder.addSource(JavaProjectBuilder.java:174)
	scala.meta.internal.mtags.JavaMtags.indexRoot(JavaMtags.scala:49)
	scala.meta.internal.metals.SemanticdbDefinition$.foreachWithReturnMtags(SemanticdbDefinition.scala:99)
	scala.meta.internal.metals.Indexer.indexSourceFile(Indexer.scala:560)
	scala.meta.internal.metals.Indexer.$anonfun$indexWorkspaceSources$7(Indexer.scala:380)
	scala.meta.internal.metals.Indexer.$anonfun$indexWorkspaceSources$7$adapted(Indexer.scala:375)
	scala.collection.IterableOnceOps.foreach(IterableOnce.scala:630)
	scala.collection.IterableOnceOps.foreach$(IterableOnce.scala:628)
	scala.collection.AbstractIterator.foreach(Iterator.scala:1313)
	scala.collection.parallel.ParIterableLike$Foreach.leaf(ParIterableLike.scala:938)
	scala.collection.parallel.Task.$anonfun$tryLeaf$1(Tasks.scala:52)
	scala.runtime.java8.JFunction0$mcV$sp.apply(JFunction0$mcV$sp.scala:18)
	scala.util.control.Breaks$$anon$1.catchBreak(Breaks.scala:97)
	scala.collection.parallel.Task.tryLeaf(Tasks.scala:55)
	scala.collection.parallel.Task.tryLeaf$(Tasks.scala:49)
	scala.collection.parallel.ParIterableLike$Foreach.tryLeaf(ParIterableLike.scala:935)
	scala.collection.parallel.AdaptiveWorkStealingTasks$AWSTWrappedTask.internal(Tasks.scala:169)
	scala.collection.parallel.AdaptiveWorkStealingTasks$AWSTWrappedTask.internal$(Tasks.scala:156)
	scala.collection.parallel.AdaptiveWorkStealingForkJoinTasks$AWSFJTWrappedTask.internal(Tasks.scala:304)
	scala.collection.parallel.AdaptiveWorkStealingTasks$AWSTWrappedTask.compute(Tasks.scala:149)
	scala.collection.parallel.AdaptiveWorkStealingTasks$AWSTWrappedTask.compute$(Tasks.scala:148)
	scala.collection.parallel.AdaptiveWorkStealingForkJoinTasks$AWSFJTWrappedTask.compute(Tasks.scala:304)
	java.base/java.util.concurrent.RecursiveAction.exec(RecursiveAction.java:194)
	java.base/java.util.concurrent.ForkJoinTask.doExec(ForkJoinTask.java:373)
	java.base/java.util.concurrent.ForkJoinPool$WorkQueue.topLevelExec(ForkJoinPool.java:1182)
	java.base/java.util.concurrent.ForkJoinPool.scan(ForkJoinPool.java:1655)
	java.base/java.util.concurrent.ForkJoinPool.runWorker(ForkJoinPool.java:1622)
	java.base/java.util.concurrent.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:165)
```
#### Short summary: 

QDox parse error in file://<WORKSPACE>/sw/java/test/java/org/forwarder/backend/impls/dl4j/opsets/aiOnnx/v13/ops/DL4JCastV13Test.java