package org.forwarder.demo;

import projectname.StreamDemoTopSim;
import scala.Tuple2;
import scala.collection.Seq;
import scala.jdk.javaapi.CollectionConverters;

import java.util.ArrayList;
import java.util.List;

public class StreamDemoTopSimJavaTest {
    public static void main(String[] args) {
        // 构造两个 4x4 矩阵，每个矩阵是 List<List<Short>>
        List<List<Short>> inputs = new ArrayList<>();

        for (int m = 0; m < 2; m++) {
            List<List<Short>> matrix = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                List<Short> row = new ArrayList<>();
                for (int j = 0; j < 4; j++) {
                    row.add((short) (m * 100 + i * 4 + j));
                }
                matrix.add(row);
            }
            inputs.addAll(matrix); // 展平成一个大列表
        }

        // 调用 Scala runSim
        Seq<Tuple2<
                Seq<Seq<scala.math.BigInt>>,
                Seq<Seq<scala.math.BigInt>>>> resultSeq = StreamDemoTopSim.runSim(inputs);

        List<Tuple2<
                Seq<Seq<scala.math.BigInt>>,
                Seq<Seq<scala.math.BigInt>>>> results = CollectionConverters.asJava(resultSeq);

        for (int m = 0; m < results.size(); m++) {
            Tuple2<Seq<Seq<scala.math.BigInt>>, Seq<Seq<scala.math.BigInt>>> pair = results.get(m);
            System.out.println("Input Matrix " + m + ":");
            printMatrix(pair._1());
            System.out.println("Output Matrix " + m + ":");
            printMatrix(pair._2());
            System.out.println();
        }
    }

    private static void printMatrix(Seq<Seq<scala.math.BigInt>> matrix) {
        for (int i = 0; i < matrix.size(); i++) {
            Seq<scala.math.BigInt> row = matrix.apply(i);
            for (int j = 0; j < row.size(); j++) {
                System.out.print(row.apply(j) + "\t");
            }
            System.out.println();
        }
    }
}
