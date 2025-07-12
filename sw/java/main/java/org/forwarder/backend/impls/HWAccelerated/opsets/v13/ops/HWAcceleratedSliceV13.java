package org.forwarder.backend.impls.HWAccelerated.opsets.v13.ops;

import java.util.List;
import java.util.ArrayList;

import org.forwarder.backend.impls.HWAccelerated.opsets.HWAcceleratedOperator;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.nd4j.linalg.indexing.INDArrayIndex;
import org.onnx4j.opsets.domain.aiOnnx.v13.ops.SliceV13;
import org.nd4j.linalg.factory.Nd4j;

import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.operator.OperatorOutputs;

public class HWAcceleratedSliceV13 extends HWAcceleratedOperator implements SliceV13{

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
        SliceInputsV13<INDArray> castedInputs = new SliceInputsV13<>(node, inputs);
        INDArray data = castedInputs.getData();

        List<Long> starts = castedInputs.toLongList(castedInputs.getStartsTensor());
        List<Long> ends = castedInputs.toLongList(castedInputs.getEndsTensor());

        List<Long> axes = (!castedInputs.hasAxes() || castedInputs.getAxesTensor().isEmpty())
                ? defaultAxes(starts.size())
                : castedInputs.toLongList(castedInputs.getAxesTensor());

        List<Long> steps = (!castedInputs.hasSteps() || castedInputs.getStepsTensor().isEmpty())
                ? defaultSteps(starts.size())
                : castedInputs.toLongList(castedInputs.getStepsTensor());

        INDArray result = this.slice(data, starts, ends, axes, steps);
        return new SliceOutputV13<>(result);
    }

    protected INDArray slice(INDArray input, List<Long> starts, List<Long> ends, List<Long> axes, List<Long> steps) {
        int rank = input.rank();
        long[] shape = input.shape();
        INDArrayIndex[] indices = new INDArrayIndex[rank];
        for (int i = 0; i < rank; i++) {
            indices[i] = NDArrayIndex.all();
        }

        List<Integer> reverseAxes = new ArrayList<>();
        boolean hasEmptySlice = false;


        for (int i = 0; i < axes.size(); i++) {
            int axis = axes.get(i) < 0 ? (int)(axes.get(i) + rank) : axes.get(i).intValue();
            long dim = shape[axis];
            long start = adjustIndex(starts.get(i), dim);
            long end = adjustIndex(ends.get(i), dim);
            long step = steps.get(i);

            boolean isEmpty = (step > 0 && start >= end) || (step < 0 && start <= end || step == 0);
            if (isEmpty) {
                hasEmptySlice = true;
                break;
            }

            if (step > 0) {
                start = Math.max(0, start);
                end = Math.min(dim, end);
                indices[axis] = NDArrayIndex.interval(start, step, end);
            } else {
                start = Math.min(dim - 1, start);
                end = Math.max(-1, end);
                long newStart = end + 1;
                long newEnd = start + 1;
                long newStep = -step;
                newStart = Math.max(0, newStart);
                newEnd = Math.min(dim, newEnd);
                indices[axis] = NDArrayIndex.interval(newStart, newStep, newEnd);
                reverseAxes.add(axis);
            }
        }

        if (hasEmptySlice) {
            long[] newShape = new long[input.rank()];
            for (int i = 0; i < newShape.length; i++) {
                newShape[i] = 0;
            }
            return Nd4j.empty(input.dataType()).reshape(newShape);
        }

        INDArray result = input.get(indices).dup();

        if (!reverseAxes.isEmpty()) {
            result = manualReverse(result, reverseAxes);
        }

        return result;
    }



//    protected INDArray slice(INDArray input, List<Long> starts, List<Long> ends, List<Long> axes, List<Long> steps) {
//        int rank = input.rank();
//        long[] shape = input.shape();
//
//        INDArrayIndex[] indices = new INDArrayIndex[rank];
//        for (int i = 0; i < rank; i++) {
//            indices[i] = NDArrayIndex.all();
//        }
//
//        for (int i = 0; i < axes.size(); i++) {
//            int axis = axes.get(i) < 0 ? (int)(axes.get(i) + rank) : axes.get(i).intValue();
//            long dim = shape[axis];
//            long start = adjustIndex(starts.get(i), dim);
//            long end = adjustIndex(ends.get(i), dim);
//            long step = steps.get(i);
//
//            // Clamp
//            if (step > 0) {
//                start = Math.max(0, start);
//                end = Math.min(dim, end);
//            } else {
//                start = Math.min(dim - 1, start);
//                end = Math.max(-1, end);
//            }
//
//            indices[axis] = NDArrayIndex.interval(start, step, end);
//        }
//
//        return input.get(indices).dup();
//    }


    private INDArray manualReverse(INDArray input, List<Integer> axesToReverse) {
        INDArray result = input;
        long[] shape = result.shape();
        int rank = shape.length;

        for (int axis : axesToReverse) {
            int axisSize = (int) shape[axis];
            List<INDArray> slices = new ArrayList<>();

            for (int i = axisSize - 1; i >= 0; i--) {
                INDArrayIndex[] sliceIndices = new INDArrayIndex[rank];
                for (int d = 0; d < rank; d++) {
                    sliceIndices[d] = NDArrayIndex.all();
                }
                sliceIndices[axis] = NDArrayIndex.point(i);

                INDArray slice = result.get(sliceIndices);
                // 注意此处：如果切片后维度降低，需恢复维度
                if (slice.rank() < rank) {
                    slice = slice.reshape(getShapeAfterPointIndexing(shape, axis));
                }

                slices.add(slice);
            }

            result = Nd4j.concat(axis, slices.toArray(new INDArray[0]));
        }

        return result;
    }

    private long[] getShapeAfterPointIndexing(long[] originalShape, int axis) {
        long[] newShape = new long[originalShape.length];
        System.arraycopy(originalShape, 0, newShape, 0, originalShape.length);
        newShape[axis] = 1;
        return newShape;
    }





    // 默认 axes 为 [0, ..., N-1]
    protected List<Long> defaultAxes(int size) {
        List<Long> result = new ArrayList<>();
        for (long i = 0; i < size; i++) {
            result.add(i);
        }
        return result;
    }

    // 默认步长为 1
    protected List<Long> defaultSteps(int size) {
        List<Long> result = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            result.add(1L);
        }
        return result;
    }

    // 支持负索引：-1 表示 dim - 1
    private long adjustIndex(long index, long dim) {
        if (index < 0) return index + dim;
        return index;
    }
}
