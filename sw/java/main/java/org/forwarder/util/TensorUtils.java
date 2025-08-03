package org.forwarder.util;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.api.buffer.DataType;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class TensorUtils {

    /**
     * 从我们自定义的二进制格式.bin文件中加载INDArray
     * @param file 要加载的文件
     * @return 加载后的INDArray
     * @throws IOException
     */
    public static INDArray loadIndArrayFromBin(File file) throws IOException {
        try (DataInputStream dis = new DataInputStream(new FileInputStream(file))) {
            // 1. 读取维度数量 (rank)
            int rank = dis.readInt();
            if (rank < 0 || rank > 10) { // 增加一个简单的检查
                throw new IOException("Invalid rank detected in tensor file: " + rank);
            }
            long[] shape = new long[rank];

            // 2. 读取每个维度的具体大小
            long numElements = 1;
            for (int i = 0; i < rank; i++) {
                shape[i] = dis.readLong();
                if (shape[i] > 0) {
                    numElements *= shape[i];
                } else {
                    numElements = 0; // 如果任何维度为0，则元素总数为0
                }
            }

            // 3. 读取所有浮点数数据
            float[] data = new float[(int)numElements];
            for (int i = 0; i < numElements; i++) {
                data[i] = dis.readFloat();
            }

            // 4. 使用形状和数据创建 INDArray
            if (numElements == 0) {
                return Nd4j.empty(DataType.FLOAT);
            } else {
                // ND4J 要求至少有一个元素才能 reshape
                return Nd4j.create(data).reshape(shape);
            }
        }
    }

}