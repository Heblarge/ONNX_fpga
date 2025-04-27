package org.forwarder.backend.impls.dl4j.opsets.aiOnnx.v11.ops;

import org.forwarder.backend.impls.dl4j.opsets.aiOnnx.DL4JAiOnnxOperator;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.impl.layers.convolution.Conv2D;
import org.nd4j.linalg.api.ops.impl.layers.convolution.config.Conv2DConfig;
import org.nd4j.linalg.api.ops.impl.layers.convolution.config.Conv2DConfig.Conv2DConfigBuilder;
import org.nd4j.linalg.api.shape.LongShapeDescriptor;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.onnx4j.Inputs;
import org.onnx4j.model.graph.Node;
import org.onnx4j.opsets.domain.aiOnnx.v11.ops.ConvV11;
import org.onnx4j.opsets.operator.OperatorOutputs;

import java.util.List;

public class DL4JConvV11 extends DL4JAiOnnxOperator implements ConvV11 {

    @Override
    public OperatorOutputs<INDArray> forward(Node node, Inputs inputs) {
        ConvV11.ConvInputsV11<INDArray> in = new ConvV11.ConvInputsV11<>(node, inputs);
        INDArray x = in.getX();
        if (x.rank() != 4) {
            throw new UnsupportedOperationException("DL4JConvV11 only supports 4D input (NCHW)");
        }
        INDArray w = in.getW();
        INDArray b = in.getB();
        String autoPad = in.getAutoPad();
        List<Long> dilations = in.getDilations();
        Long group = in.getGroup();
        List<Long> kernelShape = in.getKernelShape();
        List<Long> pads = in.getPads();
        List<Long> strides = in.getStrides();
        INDArray y = conv2d(x, w, b, autoPad, dilations, group, kernelShape, pads, strides);
        return new ConvV11.ConvOutputV11<>(y);
    }

    protected INDArray conv2d(INDArray x,
                              INDArray w,
                              INDArray b,
                              String autoPad,
                              List<Long> dilations,
                              Long group,
                              List<Long> kernelShape,
                              List<Long> pads,
                              List<Long> strides) {

        if (autoPad.startsWith("SAME_")) {
            long[] samePads = calculateSamePads(
                    x.size(2), x.size(3),
                    kernelShape.get(0), kernelShape.get(1),
                    strides.get(0), strides.get(1),
                    autoPad
            );
            x = applyAsymmetricPadding(x, samePads);
            pads = null;
        }

        if (pads != null && pads.size() >= 4 && "NOTSET".equals(autoPad)) {
            x = applyExplicitPadding(x, pads);
        }

        Conv2DConfig config = buildConvConfig(
                dilations, strides, kernelShape
        );

        INDArray wPerm = toHWCN(w, group);

        int g = group.intValue();
        if (g > 1) {
            return groupedConvolution(x, wPerm, b, g, config);
        }

        return executeConv2D(x, wPerm, b, config);
    }

    private long[] calculateSamePads(long hIn, long wIn,
                                     long kH, long kW,
                                     long sH, long sW,
                                     String autoPad) {
        long hOut = (hIn + sH - 1) / sH;
        long wOut = (wIn + sW - 1) / sW;
        long padH = Math.max((hOut - 1) * sH + kH - hIn, 0);
        long padW = Math.max((wOut - 1) * sW + kW - wIn, 0);

        long padHBegin = padH / 2;
        long padHEnd = padH - padHBegin;
        long padWBegin = padW / 2;
        long padWEnd = padW - padWBegin;

        if ("SAME_LOWER".equals(autoPad)) {
            return new long[]{padHEnd, padWEnd, padHBegin, padWBegin};
        } else { // SAME_UPPER
            return new long[]{padHBegin, padWBegin, padHEnd, padWEnd};
        }
    }

    private INDArray applyAsymmetricPadding(INDArray x, long[] pads) {
        int[][] padWidth = new int[][]{
                {0,0}, // N
                {0,0}, // C
                {(int)pads[0], (int)pads[2]}, // H
                {(int)pads[1], (int)pads[3]}  // W
        };
        return Nd4j.pad(x, padWidth);
    }

    private INDArray applyExplicitPadding(INDArray x, List<Long> pads) {
        return applyAsymmetricPadding(x, new long[]{
                pads.get(0), pads.get(1),
                pads.get(2), pads.get(3)
        });
    }

    private Conv2DConfig buildConvConfig(List<Long> dilations,
                                         List<Long> strides,
                                         List<Long> kernelShape) {
        return Conv2DConfig.builder()
                .dataFormat(Conv2DConfig.NCHW)
                .dH(dilations.get(0).intValue())
                .dW(dilations.get(1).intValue())
                .sH(strides.get(0).intValue())
                .sW(strides.get(1).intValue())
                .kH(kernelShape.get(0).intValue())
                .kW(kernelShape.get(1).intValue())
                .pH(0)
                .pW(0)
                .build();
    }

    private INDArray toHWCN(INDArray dataNCHW, long group) {
        return dataNCHW.permute(2, 3, 1, 0);
    }

    private INDArray groupedConvolution(INDArray x, INDArray wPerm,
                                        INDArray b, int group,
                                        Conv2DConfig config) {
        int C = (int) x.size(1);
        int M = (int) wPerm.size(3);
        int Cg = C / group;
        int Mg = M / group;

        INDArray[] outParts = new INDArray[group];
        for (int g = 0; g < group; g++) {
            INDArray xi = x.get(
                    NDArrayIndex.all(),
                    NDArrayIndex.interval(g * Cg, (g + 1) * Cg),
                    NDArrayIndex.all(),
                    NDArrayIndex.all()
            );

            INDArray wi = wPerm.get(
                    NDArrayIndex.all(),
                    NDArrayIndex.all(),
                    NDArrayIndex.all(),
                    NDArrayIndex.interval(g * Mg, (g + 1) * Mg)
            );

            INDArray bi = (b != null) ?
                    b.get(NDArrayIndex.interval(g * Mg, (g + 1) * Mg)) :
                    null;

            Conv2D op = new Conv2D(xi, wi, bi, null, config);
            INDArray out = Nd4j.create(op.calculateOutputShape().get(0));
            op.addOutputArgument(out);
            outParts[g] = Nd4j.exec(op)[0];
        }
        return Nd4j.concat(1, outParts);
    }

    private INDArray executeConv2D(INDArray x, INDArray wPerm,
                                   INDArray b, Conv2DConfig config) {
        Conv2D op = new Conv2D(x, wPerm, b, null, config);
        LongShapeDescriptor desc = op.calculateOutputShape().get(0);
        INDArray out = Nd4j.create(desc);
        op.addOutputArgument(out);
        return Nd4j.exec(op)[0];
    }
}