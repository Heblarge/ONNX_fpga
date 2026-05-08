#include "matrix_utils.h"
#include "xil_printf.h"
#include <math.h>
#include "xstatus.h"
#include <stdlib.h>
#include <stdio.h>

void GenerateMatrix(int32_t *matrix, int rows, int cols, unsigned int seed, int min_val, int max_val) {
    xil_printf("[GEN] Generating %dx%d Random Matrix (Range: [%d, %d], Seed: %d)...\r\n",
               rows, cols, min_val, max_val, seed);
    srand(seed);
    for (int i = 0; i < rows; i++) {
        for (int j = 0; j < cols; j++) {
            int32_t random_val = (rand() % (max_val - min_val + 1)) + min_val;
            MATRIX_ELEMENT(matrix, i, j, cols) = random_val;
        }
    }
    PrintMatrixData(matrix, rows, cols, "Generated Random Matrix");
}

void GenerateCntUpMatrix(int32_t *matrix, int rows, int cols, int offset) {
    xil_printf("[GEN] Generating %dx%d CntUp matrix with offset %d...\r\n", rows, cols, offset);
    for (int i = 0; i < rows; i++) {
        for (int j = 0; j < cols; j++) {
            MATRIX_ELEMENT(matrix, i, j, cols) = i * cols + j + offset;
        }
    }
    PrintMatrixData(matrix, rows, cols, "Generated CntUp Matrix");
}

void PrintMatrixData(int32_t *matrix, u32 rows, u32 cols, const char *name) {
    xil_printf("\r\n[PRINT] ========== %s Data (%d x %d) ==========\r\n", name, rows, cols);
    for (u32 i = 0; i < rows; i++) {
        xil_printf("Row %2d: ", i);
        for (u32 j = 0; j < cols; j++) {
            xil_printf("%6d ", matrix[i * cols + j]);
        }
        xil_printf("\r\n");
    }
    xil_printf("[PRINT] ==========================================\r\n\r\n");
}

//void GenerateGoldenMatrix(int32_t *matA, int32_t *matB, int32_t *matGolden, int rows, int cols) {
//    xil_printf("[GOLDEN] Calculating software reference (Matrix Multiply)...\r\n");
//    for (int i = 0; i < rows; i++) {
//        for (int j = 0; j < cols; j++) {
//            int32_t sum = 0;
//            for (int k = 0; k < cols; k++) {
//                int32_t valA = MATRIX_ELEMENT(matA, i, k, cols);
//                int32_t valB = MATRIX_ELEMENT(matB, k, j, cols);
//                sum += valA * valB;
//            }
//            MATRIX_ELEMENT(matGolden, i, j, cols) = sum;
//        }
//    }
//    PrintMatrixData(matGolden, rows, cols, "Golden Reference");
//    xil_printf("[GOLDEN] Calculation Done.\r\n");
//}
//void GenerateGoldenMatrix(int32_t *matA, int32_t *matB, int32_t *matG,
//                          int M, int K, int N) {
//    xil_printf("[GOLDEN] Calculating reference for %dx%d * %dx%d...\r\n", M, K, K, N);
//
//    for (int i = 0; i < M; i++) {
//        for (int j = 0; j < N; j++) {
//            // 使用 64 位累加器防止 32 位乘法结果溢出
//            int64_t sum = 0;
//            for (int k = 0; k < K; k++) {
//                // A 的索引: i * A的列数(K) + k
//                // B 的索引: k * B的列数(N) + j
//                int32_t valA = matA[i * K + k];
//                int32_t valB = matB[k * N + j];
//                sum += (int64_t)valA * valB;
//            }
//            // 结果矩阵 G 的索引: i * G的列数(N) + j
//            matG[i * N + j] = (int32_t)sum;
//        }
//    }
//    PrintMatrixData(matG, M, N, "Golden Reference");
//    xil_printf("[GOLDEN] Calculation Done.\r\n");
//}
//void GenerateGoldenMatrix(int32_t *matA, int32_t *matB, int32_t *matG,
//                          int M, int K, int N, int shift) {
//    for (int i = 0; i < M; i++) {
//        for (int j = 0; j < N; j++) {
//            // 必须使用有符号 64 位累加器
//            int64_t sum = 0;
//            for (int k = 0; k < K; k++) {
//                // int32_t 本身就是有符号的
//                int32_t valA = matA[i * K + k];
//                int32_t valB = matB[k * N + j];
//                sum += (int64_t)valA * valB; // 有符号乘加
//            }
//            // 存入结果，符号位会自动保留
//            matG[i * N + j] = (int32_t)(sum >> shift);
//        }
//    }
//}
//void GenerateGoldenMatrix(int32_t *matA, int32_t *matB, int32_t *matG,
//                          int M, int K, int N, int shift) {
//    xil_printf("[GOLDEN] Calculating with Shift: %d\r\n", shift);
//
//    for (int i = 0; i < M; i++) {
//        for (int j = 0; j < N; j++) {
//            int64_t sum = 0;
//            for (int k = 0; k < K; k++) {
//                // 确保 matA 和 matB 强转为 int64_t 再相乘，防止中间溢出
//                sum += (int64_t)matA[i * K + k] * matB[k * N + j];
//            }
//
//            // 执行矩阵移位
//            // 如果硬件设计是右移，则用 >>
//            matG[i * N + j] = (int32_t)(sum >> shift);
//        }
//    }
//    PrintMatrixData(matG, M, N, "Golden Reference (Shifted)");
//}
//下面这一段是测shift的
//void GenerateGoldenMatrix(int32_t *matA, int32_t *matB, int32_t *matG,
//                          int M, int K, int N, int shift) {
//    xil_printf("[GOLDEN] Calculating with Shift: %d\r\n", shift);
//
//    for (int i = 0; i < M; i++) {
//        for (int j = 0; j < N; j++) {
//            int64_t sum = 0;
//            for (int k = 0; k < K; k++) {
//                sum += (int64_t)matA[i * K + k] * matB[k * N + j];
//            }
//
//            // 核心逻辑修改：判断正负
//            if (shift >= 0) {
//                // 正数执行右移 (缩小)
//                matG[i * N + j] = (int32_t)(sum >> shift);
//            } else {
//                // 负数执行左移 (放大)
//                // 使用 -shift 将负值转为正的移动位数
//                matG[i * N + j] = (int32_t)(sum << (-shift));
//            }
//        }
//    }
//    PrintMatrixData(matG, M, N, "Golden Reference");
//}
//void GenerateGoldenMatrix(int32_t *matA, int32_t *matB, int32_t *matG,
//                          int M, int K, int N, int shift, int doTranspose) {
//    xil_printf("[GOLDEN] Calculating with Transpose: %d\r\n", doTranspose);
//
//    for (int i = 0; i < M; i++) {
//        for (int j = 0; j < N; j++) {
//            int64_t sum = 0;
//            for (int k = 0; k < K; k++) {
//                int32_t valA = matA[i * K + k];
//                int32_t valB;
//
//                if (doTranspose == 1) {
//                    valB = matB[j * K + k];
//                } else {
//                    valB = matB[k * N + j];
//                }
//                sum += (int64_t)valA * valB;
//            }
//            matG[i * N + j] = (int32_t)(sum >> shift);
//        }
//    }
//}
//下面这一段是测转置的
//void GenerateGoldenMatrix(int32_t *matA, int32_t *matB, int32_t *matG,
//                          int M, int K, int N, int shift, int doTranspose) {
//    // 临时存储未转置的结果
//    // 注意：结果维度是 M x N
//    int32_t temp_res[M * N];
//
//    xil_printf("[GOLDEN] Calculating A*B and Transposing G: %d\r\n", doTranspose);
//
//    // 1. 先做正常的矩阵乘法 (A * B)
//    for (int i = 0; i < M; i++) {
//        for (int j = 0; j < N; j++) {
//            int64_t sum = 0;
//            for (int k = 0; k < K; k++) {
//                sum += (int64_t)matA[i * K + k] * matB[k * N + j];
//            }
//            temp_res[i * N + j] = (int32_t)(sum >> shift);
//        }
//    }
//
//    // 2. 根据指令决定是否对输出结果 G 进行转置
//    if (doTranspose == 1) {
//        for (int i = 0; i < M; i++) {
//            for (int j = 0; j < N; j++) {
//                // 将原本 (i, j) 的位置换到 (j, i)
//                // 注意：只有当 M == N 时，转置后的形状才不变
//                matG[j * M + i] = temp_res[i * N + j];
//            }
//        }
//    } else {
//        memcpy(matG, temp_res, sizeof(temp_res));
//    }
//}
//下面这一段是测其他三种op的
//void GenerateGoldenMatrix(int32_t *matA, int32_t *matB, int32_t *matG,
//                          int M, int K, int N, int shift, int matrixOp) {
//
//    xil_printf("[GOLDEN] Op:%d, Shift:%d, Act:%d (M=K=N=16)\r\n", matrixOp, shift);
//
//    for (int i = 0; i < M; i++) {
//        for (int j = 0; j < N; j++) {
//            int64_t result = 0;
//
//            // 由于 M=K=N=16，索引公式统一使用 i * 16 + j 即可
//            int index = i * 16 + j;
//
//            switch(matrixOp) {
//                case 0: // MatMul (内积)
//                    for (int k = 0; k < 16; k++) {
//                        // 矩阵乘法特有的索引：A的行 * B的列
//                        result += (int64_t)matA[i * 16 + k] * matB[k * 16 + j];
//                    }
//                    break;
//                case 1: // ElementAdd (逐元素加)
//                    // A 和 B 对应位置相加
//                    result = (int64_t)matA[index] + matB[index];
//                    break;
//                case 2: // ElementMul (逐元素乘)
//                    // A 和 B 对应位置相乘
//                    result = (int64_t)matA[index] * matB[index];
//                    break;
//                case 3: // ElementMax (逐元素取最大值)
//                    // 比较 A 和 B 对应位置的大小
//                    result = (matA[index] > matB[index]) ? (int64_t)matA[index] : (int64_t)matB[index];
//                    break;
//            }
//            matG[index] = (int32_t)result;
//        }
//    }
//}
//接下来这一段是要没有shift和transpose之后，结合op测四种激活函数的结果
void GenerateGoldenMatrix(int32_t *matA, int32_t *matB, int32_t *matG,
                          int M, int K, int N, int shift, int matrixOp, int actFunc) {

    xil_printf("\r\n[GOLDEN] --- Start Calculation ---\r\n");
    xil_printf("[GOLDEN] Op:%d, Act:%d\r\n", matrixOp, actFunc);

    for (int i = 0; i < M; i++) {
        for (int j = 0; j < N; j++) {
            int64_t result = 0;
            int index = i * N + j;

            // 1. 矩阵运算核心 (M=K=N=16)
            switch(matrixOp) {
                case 0: // MatMul
                    for (int k = 0; k < K; k++) {
                        result += (int64_t)matA[i * K + k] * matB[k * N + j];
                    }
                    break;
                case 1: // ElementAdd
                    result = (int64_t)matA[index] + matB[index];
                    break;
                case 2: // ElementMul
                    result = (int64_t)matA[index] * matB[index];
                    break;
                case 3: // ElementMax
                    result = (matA[index] > matB[index]) ? (int64_t)matA[index] : (int64_t)matB[index];
                    break;
            }

            // --- 激活前输出 (只打印前几个元素示例) ---
            if (i == 0 && j < 4) {
                xil_printf("  [Element %d] Pre-Act Raw: %ld\r\n", j, (long)result);
            }

            // 2. 模拟硬件 Q12 处理
            // 假设激活函数吃 Q12，这里将 raw result 转为浮点数
            // 注意：因为不使用 shift，直接假设当前 result 就是 Q12 比例
            double fp_val = (double)result / 4096.0;
            double activated_fp = 0;

            // 3. 四种激活函数逻辑
            switch(actFunc) {
                            case 0: // Exp: 定义域 [-3, 2]
                                if (fp_val < -3.0) fp_val = -3.0;
                                if (fp_val >= 2.0) fp_val = 2.0;
                                activated_fp = exp(fp_val);
                                break;
                            case 1: // Ln: 定义域 [1, 6]
                                if (fp_val < 1.0) fp_val = 1.0;
                                if (fp_val >= 6.0) fp_val = 6.0;
                                activated_fp = log(fp_val);
                                break;
                            case 2: // Softplus: [-16, 16]
                                if (fp_val < -16.0) fp_val = -16.0;
                                if (fp_val >= 16.0) fp_val = 16.0;
                                activated_fp = log(1.0 + exp(fp_val));
                                break;
                            case 3: // ReLU: [-11, 6]
                                if (fp_val < -11.0) fp_val = -11.0;
                                if (fp_val >= 6.0) fp_val = 6.0;
                                activated_fp = (fp_val < 0) ? 0 : fp_val;
                                break;
                            case 4: //不做任何处理
                            	activated_fp = fp_val;
							break;
                        }

                        // 4. 写回 Q12 定点
            			int32_t final_fixed = (int32_t)(activated_fp * 4096.0);
                        matG[index] = (int32_t)(activated_fp * 4096.0);
						matG[index] = final_fixed;

            // --- 激活后输出 ---
            if (i == 0 && j < 4) {
                xil_printf("  [Element %d] Post-Act (Fixed): %d\r\n", j, final_fixed);
            }
        }
    }
    xil_printf("[GOLDEN] --- Calculation Finished ---\r\n\r\n");
}

int CompareMatrices(int32_t *matHW, int32_t *matRef, int rows, int cols) {
    int errors = 0;
    xil_printf("\r\n[CHECK] Verifying Hardware Result vs Golden Reference...\r\n");
    for (int i = 0; i < rows; i++) {
        for (int j = 0; j < cols; j++) {
            int32_t hw_val = matHW[i * cols + j];
            int32_t ref_val = matRef[i * cols + j];
            if (hw_val != ref_val) {
                if (errors < 10) {
                    xil_printf("  [ERROR] Mismatch at Row %2d, Col %2d: HW Read = %d, Golden = %d\r\n",
                               i, j, hw_val, ref_val);
                }
                errors++;
            }
        }
    }
    if (errors == 0) {
        xil_printf("[CHECK] PASS: Hardware result matches Golden Reference perfectly!\r\n");
        return XST_SUCCESS;
    } else {
        xil_printf("[CHECK] FAIL: Found %d mismatches in total.\r\n", errors);
        if (errors > 10) xil_printf("  (Only first 10 errors were printed)\r\n");
        return XST_FAILURE;
    }
}
