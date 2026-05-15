/********************************************************
 * Triple Data Mover Matrix Computation Application
 * Platform  : Zynq UltraScale+ (standalone BSP)
 * Structure : Modularized
 ********************************************************/

#include "config.h"
#include "test/matrix_utils.h"
#include "drivers/datamover_wrapper.h"
#include "drivers/sys_intr.h"
#include "drivers/accelerator.h"
#include "xil_cache.h"
#include "xil_io.h"
#include <string.h>

// 在PS DDR中分配矩阵（使用__attribute__对齐到cache line）
static int32_t matrixA[MATRIX_ROWS * MATRIX_COLS] __attribute__((aligned(64)));
static int32_t matrixB[MATRIX_ROWS * MATRIX_COLS] __attribute__((aligned(64)));
static int32_t matrixC[MATRIX_ROWS * MATRIX_COLS] __attribute__((aligned(64)));
static int32_t matrixGolden[MATRIX_ROWS * MATRIX_COLS] __attribute__((aligned(64)));

static void delay_us(u32 us) {
    volatile u32 count = us * 1000; // 粗略延时
    while (count--);
}

int main(void) {
    int Status;
    uint8_t cmd_buffer[15];

    xil_printf("\r\n========================================\r\n");
    xil_printf("      Accelerator System Starting...    \r\n");
    xil_printf("========================================\r\n");

    xil_printf("[MAIN] Wait trigger 1 (Start Generation)\r\n");
    inbyte();

    /* 步骤1: 生成数据 */
    GenerateMatrix(matrixA, MATRIX_ROWS, MATRIX_COLS, 1, 1, 9);
    GenerateMatrix(matrixB, MATRIX_ROWS, MATRIX_COLS, 2, 1, 9);
    GenerateGoldenMatrix(matrixA, matrixB, matrixGolden, MATRIX_ROWS, MATRIX_COLS);

    /* 步骤2: 刷新 Cache */
    xil_printf("[MAIN] Flushing cache...\r\n");
    Xil_DCacheFlushRange((UINTPTR)matrixA, MATRIX_SIZE);
    Xil_DCacheFlushRange((UINTPTR)matrixB, MATRIX_SIZE);

    xil_printf("[MAIN] Wait trigger 2 (Init Drivers)\r\n");
    inbyte();

    /* 步骤3 & 4: 初始化驱动与中断 */
    if (InitDataMover(&DataMover0, DATA_MOVER_0_DEV_ID, "DataMover0") != XST_SUCCESS) return XST_FAILURE;
    if (InitDataMover(&DataMover1, DATA_MOVER_1_DEV_ID, "DataMover1") != XST_SUCCESS) return XST_FAILURE;
    if (InitDataMover(&DataMover2, DATA_MOVER_2_DEV_ID, "DataMover2") != XST_SUCCESS) return XST_FAILURE;

    if (SetupIntrSystem() != XST_SUCCESS) return XST_FAILURE;

    xil_printf("[MAIN] Wait trigger 3 (Config Lifecycle)\r\n");
    inbyte();

    /* 步骤5: 配置 Cache 生命周期 */
    ConfigureCacheLifecycle(1, 1);
    xil_printf("Life-cycle Set!\r\n");

    xil_printf("[MAIN] Wait trigger 4 (Start Transfer)\r\n");
    inbyte();

    /* 步骤6: 搬运输入数据 (PS -> BRAM) */
    Status = TransferData(&DataMover0, &TransferDone0, (UINTPTR)matrixA, BRAM0_BASE,
                          MATRIX_ROWS, MATRIX_COLS * sizeof(int32_t), "DataMover0");
    if (Status != XST_SUCCESS) return XST_FAILURE;

    Status = TransferData(&DataMover1, &TransferDone1, (UINTPTR)matrixB, BRAM1_BASE,
                          MATRIX_ROWS, MATRIX_COLS * sizeof(int32_t), "DataMover1");
    if (Status != XST_SUCCESS) return XST_FAILURE;

    /* 步骤7: 等待搬运完成 */
    WaitForTransfer(&TransferDone0, "DataMover0");
    WaitForTransfer(&TransferDone1, "DataMover1");

    /* 步骤8: 发送计算指令 */
    xil_printf("[MAIN] Sending Instruction...\r\n");
    // MatrixOp=0, Shift=0, Transpose=0, Activation=4(None), Shapes=32x32
    make_command_data(cmd_buffer, 0x12345, 0, 0, 0, 4, 0, 32, 32, 32, 32, 0, 0);
    write_instruction(cmd_buffer);

    /* 步骤9: 等待计算 */
    xil_printf("[MAIN] Waiting for computation...\r\n");
    delay_us(500);

    /* 步骤10: 搬回结果 (BRAM -> PS) */
    memset(matrixC, 0, MATRIX_SIZE);
    Xil_DCacheFlushRange((UINTPTR)matrixC, MATRIX_SIZE); // 确保 matrixC 初始化为0已写入内存

    Status = TransferData(&DataMover2, &TransferDone2, BRAM2_BASE, (UINTPTR)matrixC,
                          MATRIX_ROWS, MATRIX_COLS * sizeof(int32_t), "DataMover2");
    if (Status != XST_SUCCESS) return XST_FAILURE;

    WaitForTransfer(&TransferDone2, "DataMover2");

    /* 步骤11: 读取结果并校验 */
    xil_printf("[MAIN] Reading results...\r\n");
    Xil_DCacheInvalidateRange((UINTPTR)matrixC, MATRIX_SIZE);

    PrintMatrixData(matrixC, MATRIX_ROWS, MATRIX_COLS, "Result Matrix C");

    /* 步骤12: 自动校验 */
    Status = CompareMatrices(matrixC, matrixGolden, MATRIX_ROWS, MATRIX_COLS);

    xil_printf("\r\n========================================\r\n");
    if (Status == XST_SUCCESS) {
        xil_printf("      TEST COMPLETED SUCCESSFULLY       \r\n");
        xil_printf("             [ PASS ]                   \r\n");
    } else {
        xil_printf("      TEST COMPLETED WITH ERRORS        \r\n");
        xil_printf("             [ FAIL ]                   \r\n");
    }
    xil_printf("========================================\r\n");

    return Status;
}
