error id: file://<WORKSPACE>/hw/spinal/Accelerator/InstJavaTODO.java
file://<WORKSPACE>/hw/spinal/Accelerator/InstJavaTODO.java
### com.thoughtworks.qdox.parser.ParseException: syntax error @[48,1]

error in qdox parser
file content:
```java
offset: 1560
uri: file://<WORKSPACE>/hw/spinal/Accelerator/InstJavaTODO.java
text:
```scala
// package Accelerator;

// import spinal.core.U;

// public class InstJavaTODO {
//     public int UID;
//     public String matrixOperation;
//     public int shiftLeft_AfterMatrixOperation;
//     public boolean doTranspose;
//     public String activationFunction;
//     public int shiftLeft_AfterActivation;
//     public int input0Address;
//     public int input1Address;
//     public int outputAddress;
//     public int input0Shape0;
//     public int input0Shape1;
//     public int input1Shape1;

//     public InstJavaTODO(
//         int UID,
//         String matrixOperation,
//         int shiftLeft_AfterMatrixOperation,
//         boolean doTranspose,
//         String activationFunction,
//         int shiftLeft_AfterActivation,
//         int input0Address,
//         int input1Address,
//         int outputAddress,
//         int input0Shape0,
//         int input0Shape1,
//         int input1Shape1

//     ) {
//         this.UID = UID;
//         this.matrixOperation = matrixOperation;
//         this.shiftLeft_AfterMatrixOperation = shiftLeft_AfterMatrixOperation;
//         this.doTranspose = doTranspose;
//         this.activationFunction = activationFunction;
//         this.shiftLeft_AfterActivation = shiftLeft_AfterActivation;
//         this.input0Address = input0Address;
//         this.input1Address = input1Address;
//         this.outputAddress = outputAddress;
//         this.input0Shape0 = input0Shape0;
//         this.input0Shape1 = input0Shape1;
//         this.input1Shape1 = input1Shape1;
//     }
// }
@@
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
	scala.meta.internal.metals.Indexer.indexSourceFile(Indexer.scala:546)
	scala.meta.internal.metals.Indexer.$anonfun$reindexWorkspaceSources$3(Indexer.scala:677)
	scala.meta.internal.metals.Indexer.$anonfun$reindexWorkspaceSources$3$adapted(Indexer.scala:674)
	scala.collection.IterableOnceOps.foreach(IterableOnce.scala:630)
	scala.collection.IterableOnceOps.foreach$(IterableOnce.scala:628)
	scala.collection.AbstractIterator.foreach(Iterator.scala:1313)
	scala.meta.internal.metals.Indexer.reindexWorkspaceSources(Indexer.scala:674)
	scala.meta.internal.metals.MetalsLspService.$anonfun$onChange$2(MetalsLspService.scala:912)
	scala.runtime.java8.JFunction0$mcV$sp.apply(JFunction0$mcV$sp.scala:18)
	scala.concurrent.Future$.$anonfun$apply$1(Future.scala:691)
	scala.concurrent.impl.Promise$Transformation.run(Promise.scala:500)
	java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1136)
	java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:635)
	java.base/java.lang.Thread.run(Thread.java:840)
```
#### Short summary: 

QDox parse error in file://<WORKSPACE>/hw/spinal/Accelerator/InstJavaTODO.java