error id: file://<WORKSPACE>/sw/java/main/java/org/forwarder/backend/impls/HWAccelerated/opsets/v13/ops/HWAcceleratedMatMulV13.java:Accelerator/AcceleratorSimInterface#
file://<WORKSPACE>/sw/java/main/java/org/forwarder/backend/impls/HWAccelerated/opsets/v13/ops/HWAcceleratedMatMulV13.java
empty definition using pc, found symbol in pc: Accelerator/AcceleratorSimInterface#
empty definition using semanticdb
empty definition using fallback
non-local guesses:

offset: 86
uri: file://<WORKSPACE>/sw/java/main/java/org/forwarder/backend/impls/HWAccelerated/opsets/v13/ops/HWAcceleratedMatMulV13.java
text:
```scala
package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import Accelerator.@@AcceleratorSimInterface;
import Accelerator.InstJavaTODO;
import org.forwarder.backend.impls.HWAccelerated.opsets.HWAcceleratedQuantizedOperator;
import org.forwarder.backend.impls.HWAccelerated.utils.HWAcceleratedCollector;
import org.forwarder.backend.impls.HWAccelerated.utils.HWAcceleratedTracer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.INDArrayIndex;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.onnx4j.Inputs;
import org.onnx4j.model.Graph;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.MatMulV13;
import org.onnx4j.opsets.operator.OperatorOutputs;

import java.util.List;
import java.util.Arrays;
import static org.forwarder.backend.impls.HWAccelerated.utils.HWAcceleratorTileUtils.*;


public class HWAcceleratedMatMulV13 extends HWAcceleratedQuantizedOperator implements MatMulV13 {

    private static final int HW_DIM_MULTIPLE = 16;
    private static final int MAX_HW_ELEMENTS = 512 * 512;

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
        HWAcceleratedTracer tracer = HWAcceleratedTracer.getInstance();
        tracer.startNode(node, inputs, node.getAttrs());

        INDArray outputTensor = null;
        try {
            MatMulInputsV13<INDArray> castedInputs = new MatMulInputsV13<>(node, inputs);
            INDArray matrixA = castedInputs.getA();
            INDArray matrixB = castedInputs.getB();

            Graph graph = node.getGraph();
            List<Long> targetInputShifts = castedInputs.getFpgaInShift();
            long targetInputShiftA = targetInputShifts.get(0);
            long targetInputShiftB = targetInputShifts.get(1);
            String inputAName = node.getInputNames()[0];
            long sourceShiftA = this.getProducerOutputShift(graph, inputAName, targetInputShiftA);
            String inputBName = node.getInputNames()[1];
            long sourceShiftB = this.getProducerOutputShift(graph, inputBName, targetInputShiftB);
            long targetOutputShift = castedInputs.getFpgaOutShift().get(0);

            String nodeName = node.getName();

            outputTensor = this.matmul(
                    matrixA,
                    matrixB,
                    sourceShiftA,
                    sourceShiftB,
                    targetInputShiftA,
                    targetInputShiftB,
                    targetOutputShift,
                    nodeName
            );
        } finally {
            tracer.endNode(outputTensor);
        }
        return new MatMulOutputV13<>(outputTensor);
    }

    private int ceilToMultiple(int value, int multiple) {
        if (multiple == 0) return value;
        return ((value + multiple - 1) / multiple) * multiple;
    }

    public INDArray matmul(INDArray a, INDArray b, long sourceShiftA, long sourceShiftB, long targetInputShiftA, long targetInputShiftB, long targetOutputShift, String nodeName) {
        HWAcceleratedTracer tracer = HWAcceleratedTracer.getInstance();
        HWAcceleratedTracer.TilingInfo tilingInfo = tracer.getActiveTilingInfo();

        if (a.rank() == 2 && b.rank() == 2) {
            tilingInfo.strategy = "2D2D";
            return matmul2D(a, b, sourceShiftA, sourceShiftB, targetInputShiftA, targetInputShiftB, targetOutputShift, nodeName);
        } else if (a.rank() == 3 && b.rank() == 2) {
            tilingInfo.strategy = "3D2D";
            return matmul3D2D(a, b, sourceShiftA, sourceShiftB, targetInputShiftA, targetInputShiftB, targetOutputShift, nodeName);
        } else if (a.rank() == 3 && b.rank() == 3) {
            tilingInfo.strategy = "3D3D";
            return matmul3D(a, b, sourceShiftA, sourceShiftB, targetInputShiftA, targetInputShiftB, targetOutputShift, nodeName);
        } else {
            throw new IllegalArgumentException("Unsupported tensor rank for MatMul: A=" + a.rank() + ", B=" + b.rank());
        }
    }



    private INDArray matmul2D(INDArray a, INDArray b, long sourceShiftA, long sourceShiftB, long targetInputShiftA, long targetInputShiftB, long targetOutputShift, String nodeName) {
        long[] shapeA = a.shape();
        long[] shapeB = b.shape();

        int originalRowsA = (int) shapeA[0];
        int originalColsA = (int) shapeA[1];
        int originalRowsB = (int) shapeB[0];
        int originalColsB = (int) shapeB[1];

        if (originalColsA != originalRowsB) {
            throw new IllegalArgumentException(
                    String.format("Matrix shape mismatch for 2D MatMul: A's columns (%d) must equal B's rows (%d).", originalColsA, originalRowsB)
            );
        }

        int paddedRowsA = ceilToMultiple(originalRowsA, HW_DIM_MULTIPLE);
        int paddedColsA = ceilToMultiple(originalColsA, HW_DIM_MULTIPLE);
        int paddedColsB = ceilToMultiple(originalColsB, HW_DIM_MULTIPLE);


        INDArray paddedA = Nd4j.zeros(a.dataType(),paddedRowsA, paddedColsA);
        paddedA.put(new INDArrayIndex[]{NDArrayIndex.interval(0, originalRowsA), NDArrayIndex.interval(0, originalColsA)}, a);

        INDArray paddedB = Nd4j.zeros(b.dataType(),paddedColsA, paddedColsB);
        paddedB.put(new INDArrayIndex[]{NDArrayIndex.interval(0, originalRowsB), NDArrayIndex.interval(0, originalColsB)}, b);

        INDArray paddedResult = matMulOnAccelerator(paddedA, paddedB, paddedRowsA, paddedColsA, paddedColsB, sourceShiftA, sourceShiftB, targetInputShiftA, targetInputShiftB, targetOutputShift, nodeName);

        return paddedResult.get(NDArrayIndex.interval(0, originalRowsA), NDArrayIndex.interval(0, originalColsB));
    }

    private INDArray matmul3D2D(INDArray a, INDArray b, long sourceShiftA, long sourceShiftB, long targetInputShiftA, long targetInputShiftB, long targetOutputShift, String nodeName) {
        long batchSize = a.size(0);
        long M = a.size(1);
        long K = a.size(2);
        long N = b.size(1);

        INDArray a2D = a.reshape('c', batchSize * M, K);

        // HWAcceleratedTracer tracer = HWAcceleratedTracer.getInstance();
        // tracer.pushBatchContext("Flattened[3D->2D]");
        INDArray result2D = matmul2D(a2D, b, sourceShiftA, sourceShiftB, targetInputShiftA, targetInputShiftB, targetOutputShift, nodeName);
        // tracer.popBatchContext();

        long[] outputShape = {batchSize, M, N};
        return result2D.reshape('c', outputShape);
    }

    private INDArray matmul3D(INDArray a, INDArray b, long sourceShiftA, long sourceShiftB, long targetInputShiftA, long targetInputShiftB, long targetOutputShift, String nodeName) {
        long batchA = a.size(0);
        long batchB = b.size(0);
        long batch = Math.max(batchA, batchB);
        long m = a.size(1);
        long n = b.size(2);

        INDArray result = Nd4j.createUninitialized(a.dataType(), new long[]{batch, m, n}, 'c');
        HWAcceleratedTracer tracer = HWAcceleratedTracer.getInstance();

        for (int i = 0; i < (int) batch; i++) {
            // tracer.pushBatchContext(String.format("BatchSlice[%d]", i));
            INDArray sliceA = (batchA == 1) ? a.slice(0) : a.slice(i);
            INDArray sliceB = (batchB == 1) ? b.slice(0) : b.slice(i);
            INDArray product = matmul2D(sliceA, sliceB, sourceShiftA, sourceShiftB, targetInputShiftA, targetInputShiftB, targetOutputShift, nodeName);
            result.putSlice(i, product);
            // tracer.popBatchContext();
        }
        return result;
    }


    private INDArray matMulOnAccelerator(INDArray a, INDArray b, int rowsA, int colsA, int colsB, long sourceShiftA, long sourceShiftB, long targetInputShiftA, long targetInputShiftB, long targetOutputShift, String nodeName){
        HWAcceleratedTracer tracer = HWAcceleratedTracer.getInstance();
        HWAcceleratedTracer.TilingInfo tilingInfo = tracer.getActiveTilingInfo();

        int rescaleShiftA = (int) (sourceShiftA - targetInputShiftA);
        int rescaleShiftB = (int) (sourceShiftB - targetInputShiftB);

        long[][] fixedPointA = new long[rowsA][colsA];
        for (int i = 0; i < rowsA; i++) {
            for (int j = 0; j < colsA; j++) {
                long valA = a.getLong(i, j);
                fixedPointA[i][j] = (rescaleShiftA < 0) ? (valA << -rescaleShiftA) : (valA >> rescaleShiftA);
            }
        }

        long[][] fixedPointB = new long[colsA][colsB];
        for (int i = 0; i < colsA; i++) {
            for (int j = 0; j < colsB; j++) {
                long valB = b.getLong(i, j);
                fixedPointB[i][j] = (rescaleShiftB < 0) ? (valB << -rescaleShiftB) : (valB >> rescaleShiftB);
            }
        }

        int shiftAmount = (int) (targetInputShiftA + targetInputShiftB - targetOutputShift);

        final int K = colsA;

        int maxTileM = MAX_HW_ELEMENTS / K;
        int maxTileN = MAX_HW_ELEMENTS / K;

        if (maxTileM < HW_DIM_MULTIPLE) {
            throw new IllegalArgumentException(
                    String.format("Node %s: Cannot tile A. Matrix K dimension (%d) is too large. " +
                                    "Hardware can only support %d rows (M) with this width, but operator requires multiples of %d.",
                            nodeName, K, maxTileM, HW_DIM_MULTIPLE)
            );
        }
        if (maxTileN < HW_DIM_MULTIPLE) {
            throw new IllegalArgumentException(
                    String.format("Node %s: Cannot tile B. Matrix K dimension (%d) is too large. " +
                                    "Hardware can only support %d cols (N) with this width, but operator requires multiples of %d.",
                            nodeName, K, maxTileN, HW_DIM_MULTIPLE)
            );
        }

        int hwTileCap_M = (maxTileM / HW_DIM_MULTIPLE) * HW_DIM_MULTIPLE;
        int hwTileCap_N = (maxTileN / HW_DIM_MULTIPLE) * HW_DIM_MULTIPLE;

        tilingInfo.mDim.totalPaddedSize = rowsA;
        tilingInfo.nDim.totalPaddedSize = colsB;
        tilingInfo.kDim.totalPaddedSize = K;
        tilingInfo.mDim.tiled = rowsA > hwTileCap_M;
        tilingInfo.nDim.tiled = colsB > hwTileCap_N;

        boolean isTiled = tilingInfo.mDim.tiled || tilingInfo.nDim.tiled;
        tilingInfo.status = isTiled ? "TILING" : "NO_TILING";


        System.out.println(
                "[DEBUG] node=" + nodeName +
                        " shiftAmount=" + shiftAmount
        );


        long[][] hardwareResult = new long[rowsA][colsB];

        int m_idx = 0;
        for (int m_offset = 0; m_offset < rowsA; ) {
            int rowsRemainingM = rowsA - m_offset;
            int TILE_M = Math.min(rowsRemainingM, hwTileCap_M);
            if (TILE_M <= 0) break;

            long[][] tileA = new long[TILE_M][K];
            copyTileFromSource(tileA, fixedPointA, m_offset, 0, TILE_M, K);

            int n_idx = 0;
            for (int n_offset = 0; n_offset < colsB; ) {
                int rowsRemainingN = colsB - n_offset;
                int TILE_N = Math.min(rowsRemainingN, hwTileCap_N);
                if (TILE_N <= 0) break;

                long[][] tileB = new long[K][TILE_N];
                for (int k_idx = 0; k_idx < K; k_idx++) {
                    System.arraycopy(fixedPointB[k_idx], n_offset, tileB[k_idx], 0, TILE_N);
                }

                InstJavaTODO instruction = new InstJavaTODO(
                        0,
                        "matmul",
                        shiftAmount,
                        false,
                        "none",
                        0,
                        0,
                        0,
                        0,
                        TILE_M,
                        K,
                        TILE_N,
                        0,
                        0);

                String tileIndex = String.format("M:%d, N:%d", m_idx, n_idx);
                tracer.addTile(tileIndex, instruction);

                long[][] tileResult = AcceleratorSimInterface.runRefOneInst(tileA, tileB, instruction);
                copyTileToResult(hardwareResult, tileResult, m_offset, n_offset, TILE_M, TILE_N);

                n_offset += TILE_N;
                n_idx++;
            }
            m_offset += TILE_M;
            m_idx++;
        }

        HWAcceleratedCollector.getInstance().recordLayerUsage(nodeName, fixedPointA, fixedPointB, hardwareResult);

        long[] output = new long[rowsA * colsB];
        for (int i = 0; i < rowsA; i++) {
            for (int j = 0; j < colsB; j++) {
                output[i * colsB + j] = hardwareResult[i][j];
            }
        }

        return Nd4j.create(output, new long[]{rowsA, colsB}, a.dataType());
    }

}
```


#### Short summary: 

empty definition using pc, found symbol in pc: Accelerator/AcceleratorSimInterface#