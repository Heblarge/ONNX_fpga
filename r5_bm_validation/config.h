#ifndef CONFIG_H
#define CONFIG_H

#include "xparameters.h"

// ==================== 绾兛娆� ID 鐎规矮绠� ====================
#define DATA_MOVER_0_DEV_ID        XPAR_XDATA_MOVER_0_DEVICE_ID
#define DATA_MOVER_1_DEV_ID        XPAR_XDATA_MOVER_1_DEVICE_ID
#define DATA_MOVER_2_DEV_ID        XPAR_XDATA_MOVER_2_DEVICE_ID
#define INTC_DEV_ID                XPAR_SCUGIC_SINGLE_DEVICE_ID

#define DATA_MOVER_0_INTR_ID       XPAR_FABRIC_DATA_MOVER_0_INTERRUPT_INTR
#define DATA_MOVER_1_INTR_ID       XPAR_FABRIC_DATA_MOVER_1_INTERRUPT_INTR
#define DATA_MOVER_2_INTR_ID       XPAR_FABRIC_DATA_MOVER_2_INTERRUPT_INTR
#define RESULT_READY_INTR_ID       124U

// ==================== 閸愬懎鐡ㄩ崷鏉挎絻缁屾椽妫� ====================
// BRAM
#define BRAM0_BASE                 XPAR_AXI_BRAM_CTRL_0_S_AXI_BASEADDR
#define BRAM1_BASE                 XPAR_AXI_BRAM_CTRL_1_S_AXI_BASEADDR
#define BRAM2_BASE                 XPAR_AXI_BRAM_CTRL_2_S_AXI_BASEADDR

// 閸旂娀锟界喎娅掓稉锟� Cache 閹貉冨煑閸ｏ拷
#define INSTR_BASEADDR             XPAR_WRAPFORFPGA_0_BASEADDR
#define INSTR_FIRE_OFFSET          0x10
#define CACHE_CTRL                 XPAR_MATRIXCACHECONTROLLER_0_BASEADDR
#define CACHE_A_LIFECYCLE_OFFSET   0x00
#define CACHE_B_LIFECYCLE_OFFSET   0x04

// ==================== 閻晠妯�閸欏倹鏆� ====================
#define MATRIX_ROWS                32
#define MATRIX_COLS                32
#define MATRIX_SIZE                (MATRIX_ROWS * MATRIX_COLS * sizeof(int32_t))
#define TIMEOUT_COUNT              100000000

#endif // CONFIG_H
