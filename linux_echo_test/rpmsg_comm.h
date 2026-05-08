/*
 * rpmsg_comm.h
 *
 * RPMsg communication wrapper for R5 firmware communication
 */

#ifndef RPMSG_COMM_H
#define RPMSG_COMM_H

#include <stdint.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

// RPMsg 消息类型定义
typedef enum {
    RPMSG_CMD_NEW_INSTRUCTIONS = 0x01,   // 新指令就绪
    RPMSG_CMD_COMPLETION = 0x02,         // 计算完成
    RPMSG_CMD_ERROR = 0x03,              // 错误通知
    RPMSG_CMD_PING = 0x04,               // 心跳
    RPMSG_CMD_PONG = 0x05                // 心跳响应
} rpmsg_cmd_t;

// RPMsg 消息结构
typedef struct {
    uint32_t cmd;       // 命令类型
    uint32_t param1;    // 参数1
    uint32_t param2;    // 参数2
} rpmsg_message_t;

// 不透明的句柄类型
typedef struct rpmsg_handle rpmsg_handle_t;

/**
 * 打开 RPMsg 设备
 * @param device RPMsg 设备路径 (如 "virtio0.rpmsg-openamp-demo-channel.-1.1024")
 * @return 句柄，失败返回 NULL
 */
rpmsg_handle_t* rpmsg_open(const char* device);

/**
 * 关闭 RPMsg 设备
 * @param handle 句柄
 */
void rpmsg_close(rpmsg_handle_t* handle);

/**
 * 发送 RPMsg 消息
 * @param handle 句柄
 * @param data 数据
 * @param length 长度
 * @return 发送的字节数，失败返回 -1
 */
int rpmsg_send(rpmsg_handle_t* handle, const void* data, size_t length);

/**
 * 接收 RPMsg 消息 (阻塞)
 * @param handle 句柄
 * @param data 接收缓冲区
 * @param length 缓冲区大小
 * @param timeout_ms 超时时间 (毫秒)，0=无限等待
 * @return 接收的字节数，失败返回 -1
 */
int rpmsg_recv(rpmsg_handle_t* handle, void* data, size_t length, int timeout_ms);

/**
 * 等待特定命令 (阻塞)
 * @param handle 句柄
 * @param expected_cmd 期望的命令类型
 * @param timeout_ms 超时时间 (毫秒)，0=无限等待
 * @return 成功返回 0，超时返回 -1，错误返回 -2
 */
int rpmsg_wait_for_cmd(rpmsg_handle_t* handle, uint32_t expected_cmd, int timeout_ms);

#ifdef __cplusplus
}
#endif

#endif // RPMSG_COMM_H
