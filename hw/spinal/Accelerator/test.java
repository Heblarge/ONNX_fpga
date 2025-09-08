package Accelerator;

import java.util.Random;


public class test {
	public static void main(String[] args) {
		// 生成两个随机矩阵
		int[][] matrixA = generateRandomMatrix(4, 4);
		int[][] matrixB = generateRandomMatrix(4, 4);

		// 调用Scala的矩阵加法
		InstJavaTODO instJava = new InstJavaTODO(
				0,
				"matmul",
				0,
				false,
				"relu",
				0,
				0,
				0,
				0,
				8,
				8,
				8
		);
		int[][] result = AcceleratorSimInterface.runRefOneInst(matrixA, matrixB, instJava);

		System.out.println("Matrix A:");
		printMatrix(matrixA);

		System.out.println("\nMatrix B:");
		printMatrix(matrixB);

		System.out.println("\nResult of A + B:");
		printMatrix(result);

	}

	// 生成随机矩阵
	public static int[][] generateRandomMatrix(int rows, int cols) {
		Random random = new Random();
		int[][] matrix = new int[rows][cols];

		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				matrix[i][j] = random.nextInt(10); // 0-9的随机数
			}
		}

		return matrix;
	}

	public static void printMatrix(int[][] matrix) {
		if (matrix == null || matrix.length == 0) {
			return;
		}

		int rows = matrix.length;
		int cols = matrix[0].length;

		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				System.out.print(matrix[i][j] + " ");
			}
			System.out.println();
		}
	}
}
