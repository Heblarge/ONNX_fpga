#include "matrix_utils.h"
#include "xil_printf.h"
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

void GenerateGoldenMatrix(int32_t *matA, int32_t *matB, int32_t *matGolden, int rows, int cols) {
    xil_printf("[GOLDEN] Calculating software reference (Matrix Multiply)...\r\n");
    for (int i = 0; i < rows; i++) {
        for (int j = 0; j < cols; j++) {
            int32_t sum = 0;
            for (int k = 0; k < cols; k++) {
                int32_t valA = MATRIX_ELEMENT(matA, i, k, cols);
                int32_t valB = MATRIX_ELEMENT(matB, k, j, cols);
                sum += valA * valB;
            }
            MATRIX_ELEMENT(matGolden, i, j, cols) = sum;
        }
    }
    PrintMatrixData(matGolden, rows, cols, "Golden Reference");
    xil_printf("[GOLDEN] Calculation Done.\r\n");
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
