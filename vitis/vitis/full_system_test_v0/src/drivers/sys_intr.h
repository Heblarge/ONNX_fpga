#ifndef SYS_INTR_H
#define SYS_INTR_H

#include "xscugic.h"
#include "xil_exception.h"

// 导出全局中断控制器实例
extern XScuGic Intc;

// 【新增】导出自定义完成信号标志位 (0: 未完成, 1: 完成)
extern volatile int ResultReadyFlag;

// 设置中断系统并连接所有 DataMover
int SetupIntrSystem(void);

#endif
