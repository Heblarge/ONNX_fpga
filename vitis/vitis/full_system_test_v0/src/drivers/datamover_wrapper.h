#ifndef DATAMOVER_WRAPPER_H
#define DATAMOVER_WRAPPER_H

#include "xdata_mover.h"
#include "../config.h"

// 导出全局实例供其他文件(如中断配置)使用
extern XData_mover DataMover0, DataMover1, DataMover2;
extern volatile int TransferDone0, TransferDone1, TransferDone2;

// 初始化 DataMover
int InitDataMover(XData_mover *InstancePtr, u16 DeviceId, const char *name);

// 启动一次数据搬运
int TransferData(XData_mover *InstancePtr, volatile int *done_flag,
                 u64 src_addr, u64 dst_addr, u32 rows, u32 row_len, const char *name);

// 轮询等待传输完成
int WaitForTransfer(volatile int *done_flag, const char *name);

// 中断处理函数声明（供 sys_intr.c 使用）
void DataMover0IntrHandler(void *CallbackRef);
void DataMover1IntrHandler(void *CallbackRef);
void DataMover2IntrHandler(void *CallbackRef);

#endif
