error id: file://<WORKSPACE>/hw/spinal/Accelerator/test.java
file://<WORKSPACE>/hw/spinal/Accelerator/test.java
### com.thoughtworks.qdox.parser.ParseException: syntax error @[70,1]

error in qdox parser
file content:
```java
offset: 1460
uri: file://<WORKSPACE>/hw/spinal/Accelerator/test.java
text:
```scala
// package Accelerator;

// import java.util.Random;


// public class test {
// 	public static void main(String[] args) {
// 		// 生成两个随机矩阵
// 		int[][] matrixA = generateRandomMatrix(4, 4);
// 		int[][] matrixB = generateRandomMatrix(4, 4);

// 		// 调用Scala的矩阵加法
// 		InstJavaTODO instJava = new InstJavaTODO(
// 				0,
// 				"matmul",
// 				0,
// 				false,
// 				"relu",
// 				0,
// 				0,
// 				0,
// 				0,
// 				8,
// 				8,
// 				8
// 		);
// 		int[][] result = AcceleratorSimInterface.runRefOneInst(matrixA, matrixB, instJava);

// 		System.out.println("Matrix A:");
// 		printMatrix(matrixA);

// 		System.out.println("\nMatrix B:");
// 		printMatrix(matrixB);

// 		System.out.println("\nResult of A + B:");
// 		printMatrix(result);

// 	}

// 	// 生成随机矩阵
// 	public static int[][] generateRandomMatrix(int rows, int cols) {
// 		Random random = new Random();
// 		int[][] matrix = new int[rows][cols];

// 		for (int i = 0; i < rows; i++) {
// 			for (int j = 0; j < cols; j++) {
// 				matrix[i][j] = random.nextInt(10); // 0-9的随机数
// 			}
// 		}

// 		return matrix;
// 	}

// 	public static void printMatrix(int[][] matrix) {
// 		if (matrix == null || matrix.length == 0) {
// 			return;
// 		}

// 		int rows = matrix.length;
// 		int cols = matrix[0].length;

// 		for (int i = 0; i < rows; i++) {
// 			for (int j = 0; j < cols; j++) {
// 				System.out.print(matrix[i][j] + " ");
// 			}
// 			System.out.println();
// 		}
// 	}
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

QDox parse error in file://<WORKSPACE>/hw/spinal/Accelerator/test.java