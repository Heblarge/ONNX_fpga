/********************************************************
 * Triple Data Mover Matrix Computation Application
 * Platform  : Zynq UltraScale+ (standalone BSP)
 * Test Focus: Matrix Shift (Negative/Positive) & Transpose
 ********************************************************/

#include "config.h"
#include "test/matrix_utils.h"
#include "drivers/datamover_wrapper.h"
#include "drivers/sys_intr.h"
#include "drivers/accelerator.h"
#include "xil_cache.h"
#include "xil_io.h"
#include <string.h>

// --- 缁村害瀹氫箟 ---
#define M 16
#define K 16
#define N 16

// --- 鏁扮粍鍒嗛厤 ---
static int32_t matrixA[M * K] __attribute__((aligned(64)));
static int32_t matrixB[K * N] __attribute__((aligned(64)));
static int32_t matrixC[M * N] __attribute__((aligned(64)));
static int32_t matrixGolden[M * N] __attribute__((aligned(64)));

static void delay_us(u32 us) {
    volatile u32 count = us * 1000;
    while (count--);
}

int main(void) {
    int Status;
    uint8_t cmd_buffer[15];

    // =====================================================
    // 璁剧疆娴嬭瘯鍙傛暟锛�
    // test_shift: 璐熸暟宸︾Щ(鏀惧ぇ), 姝ｆ暟鍙崇Щ(缂╁皬)
    // test_transpose: 1 寮�鍚浆缃�, 0 鍏抽棴杞疆
    // =====================================================
    int8_t test_shift = 0;      // 绀轰緥锛氬乏绉� 2 浣� (鏀惧ぇ 4 鍊�)
    uint8_t test_transpose = 0;  // 绀轰緥锛氬紑鍚浆缃祴璇�
    uint8_t test_op = 3; // 1: Add, 2: Mul, 3: Max
    uint8_t test_act = 0;    // 0:Exp, 1:Log, 2:Softplus, 3:ReLU, 4:None

    xil_printf("\r\n========================================\r\n");
    xil_printf("      Accelerator System Starting...    \r\n");
    xil_printf("      Shift: %d, Transpose: %d          \r\n", test_shift, test_transpose);
    xil_printf("========================================\r\n");

    xil_printf("[MAIN] Wait trigger 1 (Start Generation)\r\n");
    inbyte();

    /* 姝ラ1: 鐢熸垚鏁版嵁 */
//    GenerateMatrix(matrixA, M, K, 1, -9, 9);
//    GenerateMatrix(matrixB, K, N, 2, -9, 9);
//    GenerateMatrix(matrixA, M, K, -2048, 2048); // 浜х敓 -0.5 鍒� 0.5 鐨勬暟
//    GenerateMatrix(matrixB, K, N, -2048, 2048);//娴嬫縺娲诲嚱鏁扮殑鎿嶄綔
//鍐冲畾鍏蜂綋鎯呭喌鍏蜂綋鍒嗘瀽
//relu鐨勬儏鍐�
    int min_val = -3 * 4096;
    int max_val = 2 * 4096;
    GenerateMatrix(matrixA, M, K, 0, min_val, max_val);
    GenerateMatrix(matrixB, K, N, 1, min_val, max_val);
    // 杞欢璁＄畻鍙傝�冨�硷細
    // 纭繚浼犲叆浜� 8 涓弬鏁� (M, K, N, shift, transpose)
//    GenerateGoldenMatrix(matrixA, matrixB, matrixGolden, M, K, N, test_shift, test_op);
    // 璋冪敤杞欢鍙傝�冩ā鍨�//婵�娲诲嚱鏁版墍闇�瑕佺殑鍙橀噺
    GenerateGoldenMatrix(matrixA, matrixB, matrixGolden, M, K, N, test_shift, test_op, test_act);

    /* 姝ラ2: 鍒锋柊 Cache */
    xil_printf("[MAIN] Flushing cache...\r\n");
    Xil_DCacheFlushRange((UINTPTR)matrixA, M * K * sizeof(int32_t));
    Xil_DCacheFlushRange((UINTPTR)matrixB, K * N * sizeof(int32_t));

    xil_printf("[MAIN] Wait trigger 2 (Init Drivers)\r\n");
    inbyte();

    /* 姝ラ3 & 4: 鍒濆鍖栭┍鍔� */
    if (InitDataMover(&DataMover0, DATA_MOVER_0_DEV_ID, "DataMover0") != XST_SUCCESS) return XST_FAILURE;
    if (InitDataMover(&DataMover1, DATA_MOVER_1_DEV_ID, "DataMover1") != XST_SUCCESS) return XST_FAILURE;
    if (InitDataMover(&DataMover2, DATA_MOVER_2_DEV_ID, "DataMover2") != XST_SUCCESS) return XST_FAILURE;
    if (SetupIntrSystem() != XST_SUCCESS) return XST_FAILURE;

    xil_printf("[MAIN] Wait trigger 3 (Config Lifecycle)\r\n");
    inbyte();
    ConfigureCacheLifecycle(1, 1);

    xil_printf("[MAIN] Wait trigger 4 (Start Transfer)\r\n");
    inbyte();

    /* 姝ラ6: 鎼繍杈撳叆鏁版嵁 */
    Status = TransferData(&DataMover0, &TransferDone0, (UINTPTR)matrixA, BRAM0_BASE, M, K * sizeof(int32_t), "DataMover0");
    if (Status != XST_SUCCESS) return XST_FAILURE;
    Status = TransferData(&DataMover1, &TransferDone1, (UINTPTR)matrixB, BRAM1_BASE, K, N * sizeof(int32_t), "DataMover1");
    if (Status != XST_SUCCESS) return XST_FAILURE;

    WaitForTransfer(&TransferDone0, "DataMover0");
    WaitForTransfer(&TransferDone1, "DataMover1");

    /* 姝ラ8: 鍙戦�佽绠楁寚浠� */
    xil_printf("[MAIN] Sending Instruction (Shift=%d, Trans=%d)...\r\n", test_shift, test_transpose);

    // 鍙傛暟鍚箟: cmd_buffer, uid, matrixOp, shiftAfterMatrix, doTranspose, activationFunc, shiftAfterActiv, M, K, K, N, shiftLeftA, shiftLeftB
    // 姝ゅ閲嶇偣淇敼浜嗭細
    // 绗� 4 鍙傛暟锛歵est_shift
    // 绗� 5 鍙傛暟锛歵est_transpose
//    make_command_data(cmd_buffer, 0x12345, 0, test_shift, test_transpose, 4, 0, M, K, K, N, 0, 0);
    // 鍦� make_command_data 涓紶鍏� test_op 浣滀负绗笁涓弬鏁�
	make_command_data(cmd_buffer, 0x12345, test_op, test_shift, test_transpose, test_act, 0, M, N, M, N, 0, 0);
    write_instruction(cmd_buffer);

    /* 姝ラ9: 绛夊緟璁＄畻 */
    xil_printf("[MAIN] Waiting for computation...\r\n");
    delay_us(500);

    /* 姝ラ10: 鎼洖缁撴灉 */
    memset(matrixC, 0, M * N * sizeof(int32_t));
    Xil_DCacheFlushRange((UINTPTR)matrixC, M * N * sizeof(int32_t));

    Status = TransferData(&DataMover2, &TransferDone2, BRAM2_BASE, (UINTPTR)matrixC, M, N * sizeof(int32_t), "DataMover2");
    if (Status != XST_SUCCESS) return XST_FAILURE;
    WaitForTransfer(&TransferDone2, "DataMover2");

    /* 姝ラ11: 璇诲彇缁撴灉骞跺姣旀墦鍗� */
    xil_printf("[MAIN] Reading results...\r\n");
    Xil_DCacheInvalidateRange((UINTPTR)matrixC, M * N * sizeof(int32_t));

    PrintMatrixData(matrixC, M, N, "Result Matrix C (Hardware)");
    PrintMatrixData(matrixGolden, M, N, "Golden Reference (Software)");

    /* 姝ラ12: 鑷姩鏍￠獙 */
    Status = CompareMatrices(matrixC, matrixGolden, M, N);

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
