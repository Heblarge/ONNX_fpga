package org.forwarder.backend.impls.HWAccelerated.opsets;

import org.forwarder.backend.impls.HWAccelerated.HWAcceleratedBackend;
import org.forwarder.opset.annotations.Opset;
import org.onnx4j.opsets.domain.AiOnnxOperatorSet;

@Opset(backendName = HWAcceleratedBackend.BACKEND_NAME)
public abstract class HWAcceleratedOperatorSet extends AiOnnxOperatorSet {

    public HWAcceleratedOperatorSet(int irVersion, String irVersionPrerelease, String irBuildMetadata, long opsetVersion,
                                 String docString) {
        super(irVersion, irVersionPrerelease, irBuildMetadata, opsetVersion, docString);
    }

}