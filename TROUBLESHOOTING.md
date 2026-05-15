# R5 Firmware 超时问题排查

## 问题描述
JNI侧等待FPGA完成通知时超时：
```
[error] [JNI] Timeout waiting for completion
[error] Timeout waiting for FPGA completion
```

## 已修复的问题

### 1. IPI中断未启用 (helper.c)
**问题**：IPI中断被禁用但从未启用，导致RPMsg通信可能不稳定
**修复**：添加了 `XScuGic_Enable(&xInterruptController, IPI_IRQ_VECT_ID);`

### 2. FPGA等待时间不足 (fpga_driver.c, rpmsg-accelerator.c)
**问题**：固定等待10-15ms，对于512x512矩阵不够
**修复**：根据矩阵大小动态计算等待时间

### 3. RPMsg端点地址 (rpmsg-accelerator.c)
**问题**：使用 `RPMSG_ADDR_ANY`，可能与JNI侧的 `dst=0x400` 不匹配
**修复**：修改为固定地址 `0x400`

### 4. 错误处理逻辑 (rpmsg-accelerator.c)
**问题**：指令执行失败时仍返回成功
**修复**：统计失败指令数，有失败时返回错误

### 5. 调试信息增强
**添加**：
- RPMsg回调函数详细日志
- FPGA等待时间日志
- 初始化过程日志

## 需要检查的事项

### 串口输出检查
重新编译并运行后，检查串口输出：
1. 确认IPI中断已启用
2. 确认RPMsg端点创建成功 (addr=0x400)
3. 确认收到RPMsg消息 (cmd=0x02, count=X)
4. 确认FPGA等待时间正确 (wait_us=XXX)
5. 确认发送完成通知 (Sent completion notification)

### 可能的剩余问题

1. **时序问题**：JNI侧可能在R5侧端点创建前发送消息
   - 解决方案：JNI侧应该等待R5侧准备就绪

2. **RPMsg缓冲区大小**：消息可能超过RPMsg缓冲区大小 (512B)
   - 检查消息大小：`sizeof(rpmsg_header_t) + count * sizeof(instruction_msg_t)`

3. **共享内存同步**：A53和R5之间的缓存一致性问题
   - 确认缓存操作正确

4. **FPGA硬件问题**：FPGA可能没有正确执行计算
   - 通过裸机程序验证FPGA硬件

## 测试步骤

1. 重新编译R5固件
2. 部署到板子上
3. 运行Java程序
4. 查看串口输出
5. 根据输出日志定位问题

## 预期串口输出

```
========================================
Starting FPGA Accelerator R5 Firmware...
========================================
Platform initialized successfully
Creating RPMsg virtio device...
RPMsg virtio device created
Creating rpmsg endpoint
RPMsg accelerator endpoint created successfully (addr=1024)
Waiting for vdev reset...
RPMsg callback: len=XXX, src=XXX
RPMsg cmd=0x2, count=X
Received X instructions (len=XXX)
Exec: UID=X, op=X, ...
...
```
