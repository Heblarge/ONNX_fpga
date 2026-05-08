///********************************************************
// * Triple Data Mover Matrix Computation Application
// * Platform  : Zynq UltraScale+ (standalone BSP)
// * Description:
// *   - Generate two 32x32 matrices in PS memory
// *   - DataMover0: PS DDR -> BRAM0 (Matrix A)
// *   - DataMover1: PS DDR -> BRAM1 (Matrix B)
// *   - Configure cache controller lifecycle
// *   - Send instruction to accelerator
// *   - DataMover2: BRAM2 -> PS DDR (Result Matrix)
// ********************************************************/
//
//#include "xdata_mover.h"
//#include "xscugic.h"
//#include "xil_exception.h"
//#include "xil_cache.h"
//#include "xparameters.h"
//#include "xil_io.h"
//#include <stdio.h>
//#include <string.h>
//#include <stdlib.h>
//
///* ==================== 配置参数 ==================== */
//#define DATA_MOVER_0_DEV_ID        XPAR_XDATA_MOVER_0_DEVICE_ID
//#define DATA_MOVER_1_DEV_ID        XPAR_XDATA_MOVER_1_DEVICE_ID
//#define DATA_MOVER_2_DEV_ID        XPAR_XDATA_MOVER_2_DEVICE_ID
//#define INTC_DEV_ID                XPAR_SCUGIC_SINGLE_DEVICE_ID
//#define DATA_MOVER_0_INTR_ID       XPAR_FABRIC_DATA_MOVER_0_INTERRUPT_INTR
//#define DATA_MOVER_1_INTR_ID       XPAR_FABRIC_DATA_MOVER_1_INTERRUPT_INTR
//#define DATA_MOVER_2_INTR_ID       XPAR_FABRIC_DATA_MOVER_2_INTERRUPT_INTR
//#define RESULT_READY_INTR_ID	   124U
//
//// BRAM地址空间
//#define BRAM0_BASE                 XPAR_AXI_BRAM_CTRL_1_S_AXI_BASEADDR
//#define BRAM0_SIZE                 0x100000 //32k
//#define BRAM1_BASE                 XPAR_AXI_BRAM_CTRL_2_S_AXI_BASEADDR
//#define BRAM1_SIZE                 0x100000
//#define BRAM2_BASE                 XPAR_AXI_BRAM_CTRL_3_S_AXI_BASEADDR
//#define BRAM2_SIZE                 0x100000
//
////DDR地址空间
//#define DDR_LOW_BASE    XPAR_PSU_DDR_0_S_AXI_BASEADDR   // psu_ddr_0
//#define DDR_HIGH_BASE   XPAR_PSU_DDR_1_S_AXI_BASEADDR    // psu_ddr_1 (PS DDR High)
//#define DDR_PL_BASE     XPAR_DDR4_0_C0_DDR4_MEMORY_MAP_BASEADDR    // ddr4_0 (PL DDR)
//
//// Data Mover基地址
//#define DATA_MOVER_0_BASE_ADDR     XPAR_DATA_MOVER_0_S_AXI_CONTROL_BASEADDR
//#define DATA_MOVER_0_R_BASE_ADDR   XPAR_DATA_MOVER_0_S_AXI_CONTROL_R_BASEADDR
//#define DATA_MOVER_1_BASE_ADDR     XPAR_DATA_MOVER_1_S_AXI_CONTROL_BASEADDR
//#define DATA_MOVER_1_R_BASE_ADDR   XPAR_DATA_MOVER_1_S_AXI_CONTROL_R_BASEADDR
//#define DATA_MOVER_2_BASE_ADDR     XPAR_DATA_MOVER_2_S_AXI_CONTROL_BASEADDR
//#define DATA_MOVER_2_R_BASE_ADDR   XPAR_DATA_MOVER_2_S_AXI_CONTROL_R_BASEADDR
//
//// 加速器和Cache控制器地址
//#define INSTR_BASEADDR             XPAR_WRAPFORFPGA_0_BASEADDR
//#define INSTR_FIRE_OFFSET          0x10
//#define CACHE_CTRL                 XPAR_DUALCACHE_CTRL_0_BASEADDR
//#define CACHE_A_LIFECYCLE_OFFSET  0x00
//#define CACHE_B_LIFECYCLE_OFFSET  0x04
//// 矩阵参数
//#define MATRIX_ROWS                32
//#define MATRIX_COLS                32
//#define MATRIX_SIZE                (MATRIX_ROWS * MATRIX_COLS * sizeof(int32_t))
//
//// 超时计数
//#define TIMEOUT_COUNT              100000000
//
///* ==================== 宏定义 ==================== */
//#define MATRIX_ELEMENT(matrix, i, j, cols) ((matrix)[(i) * (cols) + (j)])
//
///* ==================== 全局变量 ==================== */
//static XData_mover DataMover0, DataMover1, DataMover2;
//static XScuGic Intc;
//static volatile int TransferDone0 = 0;
//static volatile int TransferDone1 = 0;
//static volatile int TransferDone2 = 0;
//static volatile int TransferError0 = 0;
//static volatile int TransferError1 = 0;
//static volatile int TransferError2 = 0;
//
//// 在PS DDR中分配矩阵（使用__attribute__对齐到cache line）
//static int32_t matrixA[MATRIX_ROWS * MATRIX_COLS] __attribute__((aligned(64)));
//static int32_t matrixB[MATRIX_ROWS * MATRIX_COLS] __attribute__((aligned(64)));
//static int32_t matrixC[MATRIX_ROWS * MATRIX_COLS] __attribute__((aligned(64)));
//
//static int32_t matrixGolden[MATRIX_ROWS * MATRIX_COLS] __attribute__((aligned(64)));
//
///* ==================== 指令合成与写指令函数 ==================== */
//void set_bits(uint8_t *buffer, int start_bit, int length, uint64_t value) {
//    uint64_t v = value;
//    for (int i = 0; i < length; i++) {
//        int bit_pos = start_bit + i;
//        int byte_idx = bit_pos / 8;
//        int bit_idx = bit_pos % 8;
//        uint8_t bit = (v >> i) & 1;
//        if (bit)
//            buffer[byte_idx] |= (1 << bit_idx);
//        else
//            buffer[byte_idx] &= ~(1 << bit_idx);
//    }
//}
//
//void make_command_data(
//    uint8_t *cmd_buffer,
//    uint32_t uid,
//    uint8_t matrixOp,
//    int8_t shiftAfterMatrix,
//    uint8_t doTranspose,
//    uint8_t activationFunc,
//    int8_t shiftAfterActiv,
//    uint32_t input0Shape_0,
//    uint32_t input0Shape_1,
//    uint32_t input1Shape_0,
//    uint32_t input1Shape_1,
//    int8_t shiftLeft_A,
//    int8_t shiftLeft_B
//) {
//    memset(cmd_buffer, 0, 15);
//    set_bits(cmd_buffer, 0, 19, uid & 0x7FFFF);
//    set_bits(cmd_buffer, 19, 2, matrixOp & 0x3);
//    set_bits(cmd_buffer, 21, 6, (uint64_t)(shiftAfterMatrix & 0x3F));
//    set_bits(cmd_buffer, 27, 1, doTranspose & 0x1);
//    set_bits(cmd_buffer, 28, 3, activationFunc & 0x7);
//    set_bits(cmd_buffer, 31, 6, (uint64_t)(shiftAfterActiv & 0x3F));
//    set_bits(cmd_buffer, 37, 16, input0Shape_0 & 0xFFFF);
//    set_bits(cmd_buffer, 53, 16, input0Shape_1 & 0xFFFF);
//    set_bits(cmd_buffer, 69, 16, input1Shape_0 & 0xFFFF);
//    set_bits(cmd_buffer, 85, 16, input1Shape_1 & 0xFFFF);
//    set_bits(cmd_buffer, 101, 6, (uint64_t)(shiftLeft_A & 0x3F));
//    set_bits(cmd_buffer, 107, 6, (uint64_t)(shiftLeft_B & 0x3F));
//}
//
//void write_instruction(uint8_t *cmd) {
//    volatile u32 *target_ptr = (volatile u32 *)INSTR_BASEADDR;
//    uint32_t dword_data[4];
//    int num_dwords = 16 / 4;
//
//    for (int i = 0; i < num_dwords; i++) {
//        dword_data[i] = 0;
//        for (int b = 0; b < 4; b++) {
//            int byte_index = i * 4 + b;
//            if (byte_index < 16) {
//                dword_data[i] |= ((uint32_t)cmd[byte_index]) << (b * 8);
//            }
//        }
//    }
//
//    target_ptr[0] = dword_data[0];
//    target_ptr[1] = dword_data[1];
//    target_ptr[2] = dword_data[2];
//    target_ptr[3] = dword_data[3];
//
//    Xil_Out32(INSTR_BASEADDR + INSTR_FIRE_OFFSET, 0x1);
//}
//
///* ==================== 中断处理函数 ==================== */
//static void DataMover0IntrHandler(void *CallbackRef) {
//    XData_mover *InstancePtr = (XData_mover *)CallbackRef;
//    u32 IrqStatus = XData_mover_InterruptGetStatus(InstancePtr);
//
//    xil_printf("[ISR0] DataMover0 interrupt: 0x%08x\r\n", IrqStatus);
//    XData_mover_InterruptClear(InstancePtr, IrqStatus);//清除中断
//
//    if (IrqStatus & 0x1) {
//        xil_printf("[ISR0] Transfer completed\r\n");
//        TransferDone0 = 1;
//    }
//}
//
//static void DataMover1IntrHandler(void *CallbackRef) {
//    XData_mover *InstancePtr = (XData_mover *)CallbackRef;
//    u32 IrqStatus = XData_mover_InterruptGetStatus(InstancePtr);
//
//    xil_printf("[ISR1] DataMover1 interrupt: 0x%08x\r\n", IrqStatus);
//    XData_mover_InterruptClear(InstancePtr, IrqStatus);
//
//    if (IrqStatus & 0x1) {
//        xil_printf("[ISR1] Transfer completed\r\n");
//        TransferDone1 = 1;
//    }
//}
//
//static void DataMover2IntrHandler(void *CallbackRef) {
//    XData_mover *InstancePtr = (XData_mover *)CallbackRef;
//    u32 IrqStatus = XData_mover_InterruptGetStatus(InstancePtr);
//
//    xil_printf("[ISR2] DataMover2 interrupt: 0x%08x\r\n", IrqStatus);
//    XData_mover_InterruptClear(InstancePtr, IrqStatus);
//
//    if (IrqStatus & 0x1) {
//        xil_printf("[ISR2] Transfer completed\r\n");
//        TransferDone2 = 1;
//    }
//}
//
///* ==================== 中断系统初始化 ==================== */
//static int SetupIntrSystem(XScuGic *IntcInst) {
//    int Status;
//    XScuGic_Config *IntcConfig;
//
//    xil_printf("[GIC] Initializing interrupt controller...\r\n");
//
//    IntcConfig = XScuGic_LookupConfig(INTC_DEV_ID);
//    if (NULL == IntcConfig) {
//        xil_printf("[GIC] Lookup config failed\r\n");
//        return XST_FAILURE;
//    }
//
//    Status = XScuGic_CfgInitialize(IntcInst, IntcConfig, IntcConfig->CpuBaseAddress);
//    if (Status != XST_SUCCESS) {
//        xil_printf("[GIC] CfgInitialize failed\r\n");
//        return XST_FAILURE;
//    }
//
//    Xil_ExceptionInit();
//    Xil_ExceptionRegisterHandler(XIL_EXCEPTION_ID_INT,
//                                 (Xil_ExceptionHandler)XScuGic_InterruptHandler,
//                                 IntcInst);
//
//    // 连接三个DataMover的中断
//    Status = XScuGic_Connect(IntcInst, DATA_MOVER_0_INTR_ID,
//                            (Xil_InterruptHandler)DataMover0IntrHandler,
//                            &DataMover0);
//    if (Status != XST_SUCCESS) {
//        xil_printf("[GIC] Connect DataMover0 failed\r\n");
//        return XST_FAILURE;
//    }
//
//    Status = XScuGic_Connect(IntcInst, DATA_MOVER_1_INTR_ID,
//                            (Xil_InterruptHandler)DataMover1IntrHandler,
//                            &DataMover1);
//    if (Status != XST_SUCCESS) {
//        xil_printf("[GIC] Connect DataMover1 failed\r\n");
//        return XST_FAILURE;
//    }
//
//    Status = XScuGic_Connect(IntcInst, DATA_MOVER_2_INTR_ID,
//                            (Xil_InterruptHandler)DataMover2IntrHandler,
//                            &DataMover2);
//    if (Status != XST_SUCCESS) {
//        xil_printf("[GIC] Connect DataMover2 failed\r\n");
//        return XST_FAILURE;
//    }
//
//    // 使能三个中断
//    XScuGic_Enable(IntcInst, DATA_MOVER_0_INTR_ID);
//    XScuGic_Enable(IntcInst, DATA_MOVER_1_INTR_ID);
//    XScuGic_Enable(IntcInst, DATA_MOVER_2_INTR_ID);
//
//    Xil_ExceptionEnable();
//
//    xil_printf("[GIC] Interrupt system initialized successfully\r\n");
//    return XST_SUCCESS;
//}
//
///* ==================== Data Mover 初始化 ==================== */
//static int InitDataMover(XData_mover *InstancePtr, u16 DeviceId, const char *name) {
//    int Status;
//    XData_mover_Config *Config;
//
//    xil_printf("[DM] Initializing %s...\r\n", name);
//
//    Config = XData_mover_LookupConfig(DeviceId);
//    if (NULL == Config) {
//        xil_printf("[DM] Lookup config failed for %s\r\n", name);
//        return XST_FAILURE;
//    }
//
//    Status = XData_mover_CfgInitialize(InstancePtr, Config);
//    if (Status != XST_SUCCESS) {
//        xil_printf("[DM] CfgInitialize failed for %s\r\n", name);
//        return XST_FAILURE;
//    }
//
//    XData_mover_DisableAutoRestart(InstancePtr);
//    XData_mover_InterruptGlobalEnable(InstancePtr);
//    XData_mover_InterruptEnable(InstancePtr, 0x3);
//
//    xil_printf("[DM] %s initialized successfully\r\n", name);
//    return XST_SUCCESS;
//}
//
///* ==================== 生成测试矩阵 ==================== */
//
//static void GenerateMatrix(int32_t *matrix, int rows, int cols, unsigned int seed, int min_val, int max_val) {
//    xil_printf("[GEN] Generating %dx%d Random Matrix (Range: [%d, %d], Seed: %d)...\r\n",
//               rows, cols, min_val, max_val, seed);
//
//    srand(seed);
//
//    for (int i = 0; i < rows; i++) {
//        for (int j = 0; j < cols; j++) {
//            // 生成 min_val 到 max_val 之间的随机数
//            // 公式: rand() % (max - min + 1) + min
//            int32_t random_val = (rand() % (max_val - min_val + 1)) + min_val;
//
//            // 存入矩阵
//            MATRIX_ELEMENT(matrix, i, j, cols) = random_val;
//        }
//    }
//
//    // 3. 按照二维形状打印矩阵
//    xil_printf("[GEN] Matrix Content:\r\n");
//    for (int i = 0; i < rows; i++) {
//        xil_printf("Row %2d: ", i);
//        for (int j = 0; j < cols; j++) {
//            // %4d 对齐输出，适合 1-9 或 1-99 的小数字
//            xil_printf("%4d ", matrix[i * cols + j]);
//        }
//        xil_printf("\r\n");
//    }
//    xil_printf("[GEN] Matrix Generation & Print Done.\r\n");
//}
//
//static void GenerateCntUpMatrix(int32_t *matrix, int rows, int cols, int offset) {
//    xil_printf("[GEN] Generating %dx%d matrix with offset %d...\r\n", rows, cols, offset);
//    // 1. 生成矩阵数据
//    for (int i = 0; i < rows; i++) {
//        for (int j = 0; j < cols; j++) {
//            // 保持原本的生成逻辑
//            MATRIX_ELEMENT(matrix, i, j, cols) = i * cols + j + offset;
//        }
//    }
//    // 2. 按照二维形状打印矩阵
//    xil_printf("[GEN] Matrix Content:\r\n");
//    for (int i = 0; i < rows; i++) {
//        xil_printf("Row %2d: ", i); // (可选) 打印行号，方便调试
//        for (int j = 0; j < cols; j++) {
//            // 这里使用标准的一维转二维索引方式 matrix[i * cols + j]
//            // %4d 用于对齐输出，如果数字很大可以改为 %6d 或直接用 %d 配合 \t
//            xil_printf("%4d ", matrix[i * cols + j]);
//        }
//        xil_printf("\r\n"); // 每一行打印完后换行
//    }
//    xil_printf("[GEN] Matrix Generation & Print Done.\r\n");
//}
//
//
///* ==================== 配置Cache控制器 ==================== */
//static void ConfigureCacheALifecycle(uint16_t lifecycle)
//{
//
//    Xil_Out32(CACHE_CTRL + CACHE_A_LIFECYCLE_OFFSET, (uint32_t)lifecycle);
//
//}
//
//static void ConfigureCacheBLifecycle(uint16_t lifecycle)
//{
//
//    Xil_Out32(CACHE_CTRL + CACHE_B_LIFECYCLE_OFFSET, (uint32_t)lifecycle);
//
//}
///* ==================== 使用Data Mover搬运数据 ==================== */
//static int TransferData(XData_mover *InstancePtr, volatile int *done_flag,
//                       u64 src_addr, u64 dst_addr, u32 rows, u32 row_len,
//                       const char *name) {
//    u32 timeout;
//
//    xil_printf("[XFER] %s: 0x%llx -> 0x%llx (%dx%d bytes)\r\n",
//               name, src_addr, dst_addr, rows, row_len);
//
//    // 等待就绪
//    timeout = TIMEOUT_COUNT;
//    while (!XData_mover_IsReady(InstancePtr) && timeout--) {
//        if (timeout == 0) {
//            xil_printf("[XFER] ERROR: %s not ready\r\n", name);
//            return XST_FAILURE;
//        }
//    }
//
//    // 配置参数
//    XData_mover_Set_mem_src(InstancePtr, src_addr);
//    XData_mover_Set_mem_dst(InstancePtr, dst_addr);
//    XData_mover_Set_rows(InstancePtr, rows);
//    XData_mover_Set_row_len_bytes(InstancePtr, row_len);
//    XData_mover_Set_src_stride_bytes(InstancePtr, row_len);
//    XData_mover_Set_dst_stride_bytes(InstancePtr, row_len);
//    XData_mover_Set_src_offset_bytes(InstancePtr, 0);
//    XData_mover_Set_dst_offset_bytes(InstancePtr, 0);
//
//    // 重置完成标志
//    *done_flag = 0;
//
//    // 启动传输
//    XData_mover_Start(InstancePtr);
//    xil_printf("[XFER] %s started\r\n", name);
//
//    return XST_SUCCESS;
//}
//
///* ==================== 等待传输完成 ==================== */
//static int WaitForTransfer(volatile int *done_flag, const char *name) {
//    u32 timeout = TIMEOUT_COUNT;
//
//    xil_printf("[WAIT] Waiting for %s to complete...\r\n", name);
//
//    while (!(*done_flag) && timeout--) {
//        if (timeout % 10000000 == 0) {
//            xil_printf("[WAIT] %s still in progress...\r\n", name);
//        }
//    }
//
//    if (!(*done_flag)) {
//        xil_printf("[WAIT] ERROR: %s timeout\r\n", name);
//        return XST_FAILURE;
//    }
//
//    xil_printf("[WAIT] %s completed\r\n", name);
//    return XST_SUCCESS;
//}
//
//
///* ==================== 打印完整矩阵数据 ==================== */
//static void PrintMatrixData(int32_t *matrix, u32 rows, u32 cols, const char *name) {
//    xil_printf("\r\n[PRINT] ========== %s Data (%d x %d) ==========\r\n", name, rows, cols);
//
//    for (u32 i = 0; i < rows; i++) {
//        // 打印行头，方便定位 (可选)
//        xil_printf("Row %2d: ", i);
//
//        for (u32 j = 0; j < cols; j++) {
//            // 计算一维数组中的索引: index = row * total_cols + col
//            u32 index = i * cols + j;
//
//            // %6d 表示占用6个字符宽度，右对齐，保证正负数和位数不同的数字列对齐
//            // 如果你的数字特别大，可以改大这个数字，或者直接用 %d 后跟 \t
//            xil_printf("%6d ", matrix[index]);
//        }
//        // 每一行打印完后换行
//        xil_printf("\r\n");
//    }
//
//    xil_printf("[PRINT] ==========================================\r\n\r\n");
//}
//
//
///* ==================== 软件计算 Golden 数据 (矩阵乘法) ==================== */
//static void GenerateGoldenMatrix(int32_t *matA, int32_t *matB, int32_t *matGolden, int rows, int cols) {
//    xil_printf("[GOLDEN] Calculating software reference (Matrix Multiply)...\r\n");
//
//    // 确保这里的行列逻辑与你的硬件加速器一致
//    // 假设是方阵：A(rows x cols) * B(rows x cols) -> C(rows x cols)
//    // 且必须满足 A的列数 == B的行数 (这里 rows=cols=32 所以没问题)
//
//    for (int i = 0; i < rows; i++) {           // 遍历 C 的行
//        for (int j = 0; j < cols; j++) {       // 遍历 C 的列
//            int32_t sum = 0;
//            for (int k = 0; k < cols; k++) {   // 累加求和 (A的列 / B的行)
//                // 对应的数学公式: C[i][j] += A[i][k] * B[k][j]
//
//                int32_t valA = MATRIX_ELEMENT(matA, i, k, cols);
//                int32_t valB = MATRIX_ELEMENT(matB, k, j, cols);
//
//                sum += valA * valB;
//            }
//            MATRIX_ELEMENT(matGolden, i, j, cols) = sum;
//        }
//    }
//
//    // 顺便打印一下 Golden 的一部分，方便肉眼确认
//    PrintMatrixData(matGolden, rows, cols, "Golden Reference");
//    xil_printf("[GOLDEN] Calculation Done.\r\n");
//}
//
///* ==================== 结果校验函数 ==================== */
//static int CompareMatrices(int32_t *matHW, int32_t *matRef, int rows, int cols) {
//    int errors = 0;
//    xil_printf("\r\n[CHECK] Verifying Hardware Result vs Golden Reference...\r\n");
//
//    for (int i = 0; i < rows; i++) {
//        for (int j = 0; j < cols; j++) {
//            int index = i * cols + j;
//            int32_t hw_val = matHW[index];
//            int32_t ref_val = matRef[index];
//
//            if (hw_val != ref_val) {
//                // 为了避免错误太多刷屏，只打印前10个错误详情
//                if (errors < 10) {
//                    xil_printf("  [ERROR] Mismatch at Row %2d, Col %2d: HW Read = %d, Golden = %d\r\n",
//                               i, j, hw_val, ref_val);
//                }
//                errors++;
//            }
//        }
//    }
//
//    if (errors == 0) {
//        xil_printf("[CHECK] PASS: Hardware result matches Golden Reference perfectly!\r\n");
//        return XST_SUCCESS;
//    } else {
//        xil_printf("[CHECK] FAIL: Found %d mismatches in total.\r\n", errors);
//        if (errors > 10) {
//            xil_printf("  (Only first 10 errors were printed)\r\n");
//        }
//        return XST_FAILURE;
//    }
//}
//
///* ==================== 延时函数 ==================== */
//static void delay_us(u32 us) {
//    // 假设CPU频率为1GHz，每微秒1000个周期
//    volatile u32 count = us * 1000;
//    while (count--);
//}
//
///* ==================== 主程序 ==================== */
//int main(void) {
//    int Status;
//    uint8_t cmd_buffer[15];
//
//    xil_printf("[MAIN] Wait trigger 1\r\n");
//    char input = inbyte();
//    /* 步骤1: 生成两个测试矩阵 */
//    xil_printf("[MAIN] Step 1: Generating matrices in PS memory...\r\n");
//    GenerateMatrix(matrixA, MATRIX_ROWS, MATRIX_COLS, 1, 1, 9);
//    GenerateMatrix(matrixB, MATRIX_ROWS, MATRIX_COLS, 2, 1, 9);
////	GenerateMatrix(matrixA, MATRIX_ROWS, MATRIX_COLS, 0, FILL_MODE_ROW_FIRST);
////	GenerateMatrix(matrixB, MATRIX_ROWS, MATRIX_COLS, 0, FILL_MODE_COL_FIRST);
//    GenerateGoldenMatrix(matrixA,matrixB,matrixGolden,MATRIX_ROWS,MATRIX_COLS);
//
//    xil_printf("[MAIN] Matrices generated\r\n\r\n");
//
//    /* 步骤2: 刷新cache确保数据写入DDR */
//    xil_printf("[MAIN] Step 2: Flushing cache for source matrices...\r\n");
//    Xil_DCacheFlushRange((UINTPTR)matrixA, MATRIX_SIZE);
//    Xil_DCacheFlushRange((UINTPTR)matrixB, MATRIX_SIZE);
//    xil_printf("[MAIN] Cache flushed\r\n\r\n");
//
//    xil_printf("[MAIN] Wait trigger 2\r\n");
//    input = inbyte();
//    /* 步骤3: 初始化三个Data Mover */
//    xil_printf("[MAIN] Step 3: Initializing Data Movers...\r\n");
//    Status = InitDataMover(&DataMover0, DATA_MOVER_0_DEV_ID, "DataMover0");
//    if (Status != XST_SUCCESS) return XST_FAILURE;
//    Status = InitDataMover(&DataMover1, DATA_MOVER_1_DEV_ID, "DataMover1");
//    if (Status != XST_SUCCESS) return XST_FAILURE;
//    Status = InitDataMover(&DataMover2, DATA_MOVER_2_DEV_ID, "DataMover2");
//    if (Status != XST_SUCCESS) return XST_FAILURE;
//    xil_printf("Data Movers Set!\r\n");
//    /* 步骤4: 设置中断系统 */
//    xil_printf("[MAIN] Step 4: Setting up interrupt system...\r\n");
//    Status = SetupIntrSystem(&Intc);
//    if (Status != XST_SUCCESS) return XST_FAILURE;
//    xil_printf("GIC Set!\r\n");
//
//    xil_printf("[MAIN] Wait trigger 3\r\n");
//    input = inbyte();
//    /* 步骤5: 配置Cache控制器生命周期 */
//    xil_printf("[MAIN] Step 5: Configuring cache life-cycle...\r\n");
//    ConfigureCacheALifecycle(1);
//    ConfigureCacheBLifecycle(1);
//    xil_printf("Life-cycle Set!\r\n");
//
//    xil_printf("[MAIN] Wait trigger 4\r\n");
//    input = inbyte();
//    /* 步骤6: 启动DataMover0和DataMover1同时搬运 */
//    xil_printf("[MAIN] Step 6: Starting parallel transfers from PS memory...\r\n");
//    xil_printf("MatrixA Address: 0x%016llx\r\n", (u64)(UINTPTR)matrixA);
//    //UINTPTR matrixA_addr = (UINTPTR)matrixA + DDR_LOW_BASE;
//    //UINTPTR matrixB_addr = (UINTPTR)matrixB + DDR_LOW_BASE;
//    // DataMover0: PS DDR (matrixA) -> BRAM0
//    Status = TransferData(&DataMover0, &TransferDone0,
//                         (UINTPTR)matrixA, BRAM0_BASE,
//                         MATRIX_ROWS, MATRIX_COLS * sizeof(int32_t),
//                         "DataMover0 (PS_A->BRAM0)");
//    if (Status != XST_SUCCESS) return XST_FAILURE;
//
//    // DataMover1: PS DDR (matrixB) -> BRAM1
//    Status = TransferData(&DataMover1, &TransferDone1,
//                         (UINTPTR)matrixB, BRAM1_BASE,
//                         MATRIX_ROWS, MATRIX_COLS * sizeof(int32_t),
//                         "DataMover1 (PS_B->BRAM1)");
//    if (Status != XST_SUCCESS) return XST_FAILURE;
//    //
//
//    /* 步骤7: 等待两个传输完成 */
//    Status = WaitForTransfer(&TransferDone0, "DataMover0");
//    if (Status != XST_SUCCESS) return XST_FAILURE;
//
//    Status = WaitForTransfer(&TransferDone1, "DataMover1");
//    if (Status != XST_SUCCESS) return XST_FAILURE;
//
//    xil_printf("[MAIN] Both input transfers completed\r\n\r\n");
//
//    /* 步骤8: 生成并发送指令 */
//    xil_printf("[MAIN] Step 7: Sending instruction to accelerator...\r\n");
//
//    make_command_data(cmd_buffer,
//        0x12345,  // uid
//        0x0,      // matrixOp
//        0x0,      // shiftAfterMatrix
//        0x0,      // doTranspose
//        0x4,      // activationFunc
//        0x0,      // shiftAfterActiv
//        0x20,     // input0Shape_0 (32)
//        0x20,     // input0Shape_1 (32)
//        0x20,     // input1Shape_0 (32)
//        0x20,     // input1Shape_1 (32)
//        0x0,      // shiftLeft_A
//        0x0       // shiftLeft_B
//    );
//
//    write_instruction(cmd_buffer);
//    xil_printf("[MAIN] Instruction sent to accelerator\r\n\r\n");
//
//    /* 步骤9: 等待500微秒 */
//    xil_printf("[MAIN] Step 8: Waiting for computation (500us)...\r\n");
//    delay_us(500); // 500微秒，约10000个周期
//    xil_printf("[MAIN] Computation time elapsed\r\n\r\n");
//
//    /* 步骤10: 使用DataMover2搬回结果 */
//    xil_printf("[MAIN] Step 9: Transferring results back to PS memory...\r\n");
//
//    // 先清空结果矩阵
//    memset(matrixC, 0, MATRIX_SIZE);
//    Xil_DCacheFlushRange((UINTPTR)matrixC, MATRIX_SIZE);
//
//    // DataMover2: BRAM2 -> PS DDR (matrixC)
//    Status = TransferData(&DataMover2, &TransferDone2,
//                         BRAM2_BASE, (UINTPTR)matrixC,
//                         MATRIX_ROWS, MATRIX_COLS * sizeof(int32_t),
//                         "DataMover2 (BRAM2->PS_C)");
//    if (Status != XST_SUCCESS) return XST_FAILURE;
//
//    Status = WaitForTransfer(&TransferDone2, "DataMover2");
//    if (Status != XST_SUCCESS) return XST_FAILURE;
//    xil_printf("\r\n");
//
//    /* 步骤11: 失效cache并读取结果 */
//    xil_printf("[MAIN] Step 10: Invalidating cache and reading results...\r\n");
//    Xil_DCacheInvalidateRange((UINTPTR)matrixC, MATRIX_SIZE);
//
//    /* 步骤12: 打印结果 */
//    xil_printf("[MAIN] Step 11: Printing results...\r\n");
//    PrintMatrixData(matrixC, MATRIX_ROWS, MATRIX_COLS, "Result Matrix C");
//
//    /* ==================== 新增：自动校验步骤 ==================== */
//	xil_printf("[MAIN] Step 12: Automated Verification...\r\n");
//	Status = CompareMatrices(matrixC, matrixGolden, MATRIX_ROWS, MATRIX_COLS);
//
//	xil_printf("\r\n========================================\r\n");
//	if (Status == XST_SUCCESS) {
//		xil_printf("      TEST COMPLETED SUCCESSFULLY       \r\n");
//		xil_printf("             [ PASS ]                   \r\n");
//	} else {
//		xil_printf("      TEST COMPLETED WITH ERRORS        \r\n");
//		xil_printf("             [ FAIL ]                   \r\n");
//	}
//	xil_printf("========================================\r\n");
//
//	return Status;
//}
