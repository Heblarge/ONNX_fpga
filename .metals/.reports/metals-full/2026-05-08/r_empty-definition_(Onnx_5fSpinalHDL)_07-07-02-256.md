error id: file://<WORKSPACE>/sw/java/main/java/org/forwarder/backend/impls/HWAccelerated/opsets/v13/ops/HWAcceleratedAddExpV13.java:_empty_/InstJavaTODO#
file://<WORKSPACE>/sw/java/main/java/org/forwarder/backend/impls/HWAccelerated/opsets/v13/ops/HWAcceleratedAddExpV13.java
empty definition using pc, found symbol in pc: _empty_/InstJavaTODO#
semanticdb not found
empty definition using fallback
non-local guesses:

offset: 6122
uri: file://<WORKSPACE>/sw/java/main/java/org/forwarder/backend/impls/HWAccelerated/opsets/v13/ops/HWAcceleratedAddExpV13.java
text:
```scala
package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import Accelerator.AcceleratorSimInterface;
import Accelerator.InstJavaTODO;
import org.forwarder.backend.impls.HWAccelerated.opsets.HWAcceleratedQuantizedOperator;
import org.forwarder.backend.impls.HWAccelerated.utils.HWAcceleratedCollector;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.onnx4j.Inputs;
import org.onnx4j.model.Graph;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.AddExpV13;
import org.onnx4j.opsets.operator.OperatorOutputs;

import java.util.List;

public class HWAcceleratedAddExpV13 extends HWAcceleratedQuantizedOperator implements AddExpV13 {

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {

        AddExpInputsV13<INDArray> castedInputs = new AddExpInputsV13<>(node, inputs);

        INDArray A = castedInputs.getA();
        INDArray B = castedInputs.getB();

        Graph graph = node.getGraph();

        List<Long> targetInputShifts = castedInputs.getFpgaInShift();
        long targetInputShiftA = targetInputShifts.get(0);
        long targetInputShiftB = targetInputShifts.get(1);

        String inputAName = node.getInputNames()[0];
        String inputBName = node.getInputNames()[1];

        long sourceShiftA = this.getProducerOutputShift(graph, inputAName, targetInputShiftA);
        long sourceShiftB = this.getProducerOutputShift(graph, inputBName, targetInputShiftB);

        long targetOutputShift = castedInputs.getFpgaOutShift().get(0);

        INDArray output = addExp(
                A, B,
                sourceShiftA, sourceShiftB,
                targetInputShiftA, targetInputShiftB,
                targetOutputShift,
                node.getName()
        );

        return new AddExpOutputV13<>(output);
    }

    /* ============================================================
       Main Entry
       ============================================================ */

    public INDArray addExp(
            INDArray a, INDArray b,
            long sourceShiftA, long sourceShiftB,
            long targetInputShiftA, long targetInputShiftB,
            long targetOutputShift,
            String nodeName) {

        if (!java.util.Arrays.equals(a.shape(), b.shape())) {
            long[] broadcastShape = getBroadcastShape(a.shape(), b.shape());
            a = a.broadcast(broadcastShape);
            b = b.broadcast(broadcastShape);
        }

        if (a.rank() == 2) {
            return addExp2D(a, b,
                    sourceShiftA, sourceShiftB,
                    targetInputShiftA, targetInputShiftB,
                    targetOutputShift,
                    nodeName);
        } else if (a.rank() == 3) {

            long batch = a.size(0);
            INDArray result = Nd4j.createUninitialized(a.dataType(), a.shape());

            for (int i = 0; i < batch; i++) {
                INDArray sliceA = a.slice(i);
                INDArray sliceB = b.slice(i);

                INDArray sliceResult = addExp2D(sliceA, sliceB,
                        sourceShiftA, sourceShiftB,
                        targetInputShiftA, targetInputShiftB,
                        targetOutputShift,
                        nodeName);

                result.putSlice(i, sliceResult);
            }

            return result;
        }

        throw new IllegalArgumentException("Unsupported rank: " + a.rank());
    }

    /* ============================================================
       2D Core Logic with Dynamic Scaling
       ============================================================ */

    private INDArray addExp2D(
            INDArray a, INDArray b,
            long sourceShiftA, long sourceShiftB,
            long targetInputShiftA, long targetInputShiftB,
            long targetOutputShift,
            String nodeName) {

        int rows = (int) a.rows();
        int cols = (int) a.columns();

        long s_hw = AcceleratorSimInterface.acceleratorCfg().fracWidth();

        int rescaleShiftA = (int)(sourceShiftA - targetInputShiftA);
        int rescaleShiftB = (int)(sourceShiftB - targetInputShiftB);

        long[][] addResult = new long[rows][cols];

        long maxAbs = 0;

    /* ===============================
       1️⃣ Rescale + Add
       =============================== */
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {

                long valA = a.getLong(i, j);
                long valB = b.getLong(i, j);

                long fixedA = (rescaleShiftA < 0) ? (valA << -rescaleShiftA) : (valA >> rescaleShiftA);
                long fixedB = (rescaleShiftB < 0) ? (valB << -rescaleShiftB) : (valB >> rescaleShiftB);

                long sum = fixedA + fixedB;

                addResult[i][j] = sum;

                long abs = Math.abs(sum);
                if (abs > maxAbs) maxAbs = abs;
            }
        }

    /* ===============================
       2️⃣ 正确 Dynamic Scaling
       保证 exp 输入 ∈ [-3,2)
       =============================== */
        int expScaleShift = (int)(targetInputShiftA - s_hw);


        long expUpperBound = (2L << s_hw);
        long expLowerBound = (3L << s_hw);

        int dynamicRightShift = 0;

        while (true) {

            long testMax = maxAbs;

            // 模拟 exp 真实输入
            if (expScaleShift > 0)
                testMax >>= expScaleShift;
            else
                testMax <<= -expScaleShift;

            testMax >>= dynamicRightShift;

            if (testMax <= expUpperBound)
                break;

            dynamicRightShift++;
        }
        int addShiftAmount = expScaleShift;


    /* ===============================
       3️⃣ 计算 exp 所需 shift
       =============================== */

        //int addShiftAmount = (int)(targetInputShiftA - s_hw - dynamicRightShift);
        int postShiftAmount = (int)(s_hw - targetOutputShift);

        long[][] zeroMatrix = new long[rows][cols];

        InstJavaTODO instruction = new InstJavaTODO@@(
                0,
                "elementadd",
                addShiftAmount,
                false,
                "exp",
                postShiftAmount,
                0,
                0,
                0,
                rows,
                cols,
                cols,
                0,
                0
        );

        long[][] tileResult = AcceleratorSimInterface.runRefOneInst(
                addResult,
                zeroMatrix,
                instruction
        );

        HWAcceleratedCollector.getInstance()
                .recordLayerUsage(nodeName, addResult, zeroMatrix, tileResult);

        long[] flat = new long[rows * cols];

        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++)
                flat[i * cols + j] = tileResult[i][j];

        return Nd4j.create(flat, new long[]{rows, cols}, a.dataType());
    }


    private long[] getBroadcastShape(long[] shapeA, long[] shapeB) {

        int maxRank = Math.max(shapeA.length, shapeB.length);
        long[] result = new long[maxRank];

        for (int i = 1; i <= maxRank; i++) {

            long dimA = (shapeA.length - i >= 0) ? shapeA[shapeA.length - i] : 1;
            long dimB = (shapeB.length - i >= 0) ? shapeB[shapeB.length - i] : 1;

            result[maxRank - i] = Math.max(dimA, dimB);
        }

        return result;
    }
}

```


#### Short summary: 

empty definition using pc, found symbol in pc: _empty_/InstJavaTODO#