error id: file://<WORKSPACE>/sw/java/test/java/org/forwarder/backend/impls/HWAccelerated/opsets/v13/ops/HWAcceleratedNegV13Test.java
file://<WORKSPACE>/sw/java/test/java/org/forwarder/backend/impls/HWAccelerated/opsets/v13/ops/HWAcceleratedNegV13Test.java
### com.thoughtworks.qdox.parser.ParseException: syntax error @[116,4]

error in qdox parser
file content:
```java
offset: 4053
uri: file://<WORKSPACE>/sw/java/test/java/org/forwarder/backend/impls/HWAccelerated/opsets/v13/ops/HWAcceleratedNegV13Test.java
text:
```scala
//package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;
//
//import org.forwarder.backend.impls.HWAccelerated.HWAcceleratedTestCase;
//import org.junit.Test;
//import org.nd4j.linalg.api.ndarray.INDArray;
//import org.nd4j.linalg.factory.Nd4j;
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertArrayEquals;
//import static org.junit.Assert.assertTrue;
//import static org.junit.Assert.fail;
//
//
//
//import static org.junit.Assert.assertArrayEquals;
//
//public class HWAcceleratedNegV13Test extends HWAcceleratedTestCase {
//
//    private void assertINDArrayEqualsWithNaN(INDArray expected, INDArray actual, double eps) {
//        assertArrayEquals(expected.shape(), actual.shape());
//        long length = expected.length();
//        for (int i = 0; i < length; i++) {
//            double e = expected.getDouble(i);
//            double a = actual.getDouble(i);
//            if (Double.isNaN(e)) {
//                if (!Double.isNaN(a)) {
//                    fail("Expected NaN at index " + i + " but got " + a);
//                }
//            } else if (Double.isInfinite(e)) {
//                if (e != a) {
//                    fail("Expected " + e + " at index " + i + " but got " + a);
//                }
//            } else {
//                if (Math.abs(e - a) > eps) {
//                    fail("Expected " + e + " at index " + i + " but got " + a);
//                }
//            }
//        }
//    }
//
//    private void testNeg(INDArray expected, INDArray input) {
//        HWAcceleratedNegV13 op = new HWAcceleratedNegV13();
//        INDArray result = op.neg(input);
//        assertINDArrayEqualsWithNaN(expected, result, 1e-6);
//    }
//
//
//
//    @Test
//    public void testScalarInput() throws Exception {
//        INDArray input = Nd4j.scalar(5.0);
//        INDArray expected = Nd4j.scalar(-5.0);
//
//        testNeg(expected, input);
//    }
//
//    @Test
//    public void testZeroInput() throws Exception {
//        INDArray input = Nd4j.scalar(0.0);
//        INDArray expected = Nd4j.scalar(0.0); // -0 = 0
//
//        testNeg(expected, input);
//    }
//
//    @Test
//    public void testVectorInput() throws Exception {
//        INDArray input = Nd4j.create(new double[]{-1.0, 0.0, 1.0, 2.5});
//        INDArray expected = Nd4j.create(new double[]{1.0, 0.0, -1.0, -2.5});
//
//        testNeg(expected, input);
//    }
//
//    @Test
//    public void testMatrixInput() throws Exception {
//        INDArray input = Nd4j.create(new double[][]{{-10.0, 0.0}, {10.0, 100.0}});
//        INDArray expected = Nd4j.create(new double[][]{{10.0, 0.0}, {-10.0, -100.0}});
//
//        testNeg(expected, input);
//    }
//
//    @Test
//    public void testHighDimInput() throws Exception {
//        INDArray input = Nd4j.create(2, 3, 4).assign(0.5);
//        INDArray expected = Nd4j.create(2, 3, 4).assign(-0.5);
//
//        testNeg(expected, input);
//    }
//
//    @Test
//    public void testLargeValues() throws Exception {
//        // 测试大数值输入
//        INDArray largePos = Nd4j.scalar(Double.MAX_VALUE);
//        INDArray largeNeg = Nd4j.scalar(-Double.MAX_VALUE);
//
//        INDArray expectedLargePos = Nd4j.scalar(-Double.MAX_VALUE);
//        INDArray expectedLargeNeg = Nd4j.scalar(Double.MAX_VALUE);
//
//        testNeg(expectedLargePos, largePos);
//        testNeg(expectedLargeNeg, largeNeg);
//    }
//
//    @Test
//    public void testSpecialValues() throws Exception {
//        // 测试特殊值
//        INDArray nan = Nd4j.scalar(Double.NaN);
//        INDArray posInf = Nd4j.scalar(Double.POSITIVE_INFINITY);
//        INDArray negInf = Nd4j.scalar(Double.NEGATIVE_INFINITY);
//
//        INDArray expectedNan = Nd4j.scalar(Double.NaN);
//        INDArray expectedPosInf = Nd4j.scalar(Double.NEGATIVE_INFINITY);
//        INDArray expectedNegInf = Nd4j.scalar(Double.POSITIVE_INFINITY);
//
//        testNeg(expectedNan, nan);
//        testNeg(expectedPosInf, posInf);
//        testNeg(expectedNegInf, negInf);
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

QDox parse error in file://<WORKSPACE>/sw/java/test/java/org/forwarder/backend/impls/HWAccelerated/opsets/v13/ops/HWAcceleratedNegV13Test.java