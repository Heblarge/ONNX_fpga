#ifndef MATRIX_UTILS_H
#define MATRIX_UTILS_H

#include <stdint.h>
#include "xil_types.h"

// 宏定义方便访问元素
#define MATRIX_ELEMENT(matrix, i, j, cols) ((matrix)[(i) * (cols) + (j)])

// 生成随机矩阵
void GenerateMatrix(int32_t *matrix, int rows, int cols, unsigned int seed, int min_val, int max_val);

// 生成计数矩阵 (0, 1, 2...)
void GenerateCntUpMatrix(int32_t *matrix, int rows, int cols, int offset);

// 软件计算 Golden 参考结果
//void GenerateGoldenMatrix(int32_t *matA, int32_t *matB, int32_t *matGolden, int rows, int cols);
// 修改前的声明可能是 void GenerateGoldenMatrix(..., int rows, int cols);
// 修改后：
//void GenerateGoldenMatrix(int32_t *matA, int32_t *matB, int32_t *matG, int M, int K, int N);
// matrix_utils.h
//void GenerateGoldenMatrix(int32_t *matA, int32_t *matB, int32_t *matG, int M, int K, int N, int shift);
void GenerateGoldenMatrix(int32_t *matA, int32_t *matB, int32_t *matG, int M, int K, int N, int shift, int op, int act);

//void GenerateGoldenMatrix(int32_t *matA, int32_t *matB, int32_t *matG, int M, int K, int N, int shift);
// 打印矩阵内容
void PrintMatrixData(int32_t *matrix, u32 rows, u32 cols, const char *name);

// 对比两个矩阵，返回 XST_SUCCESS 或 XST_FAILURE
int CompareMatrices(int32_t *matHW, int32_t *matRef, int rows, int cols);

#endif
