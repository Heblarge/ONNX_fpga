package org.forwarder.backend.impls.HWAccelerated.utils;

import org.nd4j.linalg.api.buffer.DataType;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class HWAcceleratedDataTypeHelper {

    protected static BiMap<org.onnx4j.tensor.DataType, org.nd4j.linalg.api.buffer.DataType> DATATYPE_ONNX4J_HWAccelerated_MAP;
    protected static BiMap<org.nd4j.linalg.api.buffer.DataType, org.onnx4j.tensor.DataType> DATATYPE_HWAccelerated_ONNX4J_MAP;

    static {
        DATATYPE_ONNX4J_HWAccelerated_MAP = HashBiMap.create();
        DATATYPE_ONNX4J_HWAccelerated_MAP.put(org.onnx4j.tensor.DataType.UINT8, DataType.UBYTE);
        DATATYPE_ONNX4J_HWAccelerated_MAP.put(org.onnx4j.tensor.DataType.UINT16, DataType.UINT16);
        DATATYPE_ONNX4J_HWAccelerated_MAP.put(org.onnx4j.tensor.DataType.UINT32, DataType.UINT32);
        DATATYPE_ONNX4J_HWAccelerated_MAP.put(org.onnx4j.tensor.DataType.UINT64, DataType.UINT64);
        DATATYPE_ONNX4J_HWAccelerated_MAP.put(org.onnx4j.tensor.DataType.INT8, DataType.BYTE);
        DATATYPE_ONNX4J_HWAccelerated_MAP.put(org.onnx4j.tensor.DataType.INT16, DataType.SHORT);
        DATATYPE_ONNX4J_HWAccelerated_MAP.put(org.onnx4j.tensor.DataType.INT32, DataType.INT);
        DATATYPE_ONNX4J_HWAccelerated_MAP.put(org.onnx4j.tensor.DataType.INT64, DataType.LONG);
        DATATYPE_ONNX4J_HWAccelerated_MAP.put(org.onnx4j.tensor.DataType.FLOAT16, DataType.HALF);
        DATATYPE_ONNX4J_HWAccelerated_MAP.put(org.onnx4j.tensor.DataType.FLOAT, DataType.FLOAT);
        DATATYPE_ONNX4J_HWAccelerated_MAP.put(org.onnx4j.tensor.DataType.DOUBLE, DataType.DOUBLE);
        DATATYPE_ONNX4J_HWAccelerated_MAP.put(org.onnx4j.tensor.DataType.BOOL, DataType.BOOL);
        DATATYPE_ONNX4J_HWAccelerated_MAP.put(org.onnx4j.tensor.DataType.STRING, DataType.UTF8);

        DATATYPE_HWAccelerated_ONNX4J_MAP = DATATYPE_ONNX4J_HWAccelerated_MAP.inverse();
    }

    public static org.nd4j.linalg.api.buffer.DataType toHWAcceleratedDataType(org.onnx4j.tensor.DataType onnx4jDataType) {
        DataType datatype = DATATYPE_ONNX4J_HWAccelerated_MAP.get(onnx4jDataType);
        if (datatype == null)
            throw new UnsupportedOperationException(
                    String.format("DataType \"%s\" not be supported in ND4J", onnx4jDataType));
        else
            return datatype;
    }

    public static org.onnx4j.tensor.DataType toOnnx4jDataType(org.nd4j.linalg.api.buffer.DataType dl4jDataType) {
        org.onnx4j.tensor.DataType datatype = DATATYPE_HWAccelerated_ONNX4J_MAP.get(dl4jDataType);
        if (datatype == null)
            throw new UnsupportedOperationException(
                    String.format("Nd4j data type \"%s\" not be supported in Onnx4j", dl4jDataType));
        else
            return datatype;
    }

    public static void ensureNotContainUnexceptedType(DataType currentType, DataType[] HWAcceleratedUnexceptedTypes) {
        for (DataType HWAcceleratedUnexceptedType : HWAcceleratedUnexceptedTypes) {
            if (HWAcceleratedUnexceptedType.equals(currentType))
                throw new RuntimeException(String.format("Nd4j data type \"\" is invalid type in this operation"));
        }
    }

}
