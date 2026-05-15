/*
 * accelerator_jni.cpp
 *
 * JNI bridge between Java and R5/FPGA accelerator using OpenAMP RPMsg
 *
 * Communication flow:
 * - Java → JNI → write(/dev/rpmsgX) → kernel driver → vring + IPI → R5
 * - Shared memory pool mapped via /dev/mem for efficient data transfer
 *
 * Instruction format aligned with r5_bm_validation/drivers/accelerator.c
 *
 * Shared Memory Layout (must match rsc_table.c):
 *   Block 0: 0x3F100000, 10MB
 *   Block 1: 0x3FB00000, 10MB
 *   Block 2: 0x40500000, 10MB
 *   Block 3: 0x40F00000, 10MB
 *
 * Compile: g++ -shared -fPIC -o libaccelerator_jni.so \
 *              -I${JAVA_HOME}/include -I${JAVA_HOME}/include/linux \
 *              accelerator_jni.cpp rpmsg_comm.c -lpthread
 */

#include <jni.h>
#include <fcntl.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <pthread.h>
#include <poll.h>
#include <sys/ioctl.h>
#include <sys/mman.h>
#include <linux/rpmsg.h>
#include <dirent.h>

#include "rpmsg_comm.h"

// ========== 共享内存配置 ==========
#define MAX_SHM_BLOCKS 4
#define MEM_DEV "/dev/mem"

// 共享内存块配置 (与 r5_firmware/rsc_table.h 保持一致)
static const struct {
    unsigned long phys_addr;
    size_t size;
} SHM_CONFIG[MAX_SHM_BLOCKS] = {
    {0x3F100000UL, 0x00A00000},  // Block 0: 10MB
    {0x3FB00000UL, 0x00A00000},  // Block 1: 10MB
    {0x40500000UL, 0x00A00000},  // Block 2: 10MB
    {0x40F00000UL, 0x00A00000},  // Block 3: 10MB
};

// RPMsg 消息头
typedef struct {
    uint32_t cmd;       // 命令类型
    uint32_t count;     // 指令数量
} __attribute__((packed)) rpmsg_header_t;

#define MAX_INSTRUCTIONS 256  // 单次发送最大指令数

// RPMsg 命令 (使用 rpmsg_comm.h 中的定义)
#define RPMSG_CMD_INSTRUCTIONS_DATA 0x02  // 直接发送指令数据
#define RPMSG_CMD_QUERY_STATUS      0x05  // 查询状态
#define RPMSG_CMD_WRITE_DDR         0x06  // 写入 DDR
#define RPMSG_CMD_READ_DDR          0x07  // 读取 DDR

// FPGA 状态定义
#define FPGA_STATUS_IDLE        0
#define FPGA_STATUS_BUSY        1
#define FPGA_STATUS_ERROR       2

// ==================== 指令结构 (对应 InstJavaTODO.java) ====================
// 与 Accelerator/InstJavaTODO.java 完全对齐
// 使用预分配buffer池方案：bufferId用于索引R5侧的buffer池
//
// Java: InstJavaTODO(int UID, String matrixOperation, int shiftLeft_AfterMatrixOperation,
//                    boolean doTranspose, String activationFunction, int shiftLeft_AfterActivation,
//                    int bufferIdA, int bufferIdB, int bufferIdZ,
//                    int input0Shape0, int input0Shape1, int input1Shape1,
//                    int shiftLeft_A, int shiftLeft_B)
//
typedef struct {
    int32_t  UID;                           // 唯一标识符
    uint8_t  matrixOperation;               // 0=MatMul, 1=ElementAdd, 2=ElementMul, 3=ElementMax
    int8_t   shiftLeft_AfterMatrixOperation;
    uint8_t  doTranspose;                   // 0=false, 1=true
    uint8_t  activationFunction;            // 0=Exp, 1=Log, 2=Softplus, 3=Relu, 4=None
    int8_t   shiftLeft_AfterActivation;
    int32_t  bufferIdA;                     // 输入A的buffer索引 (0-15)
    int32_t  bufferIdB;                     // 输入B的buffer索引 (0-15)
    int32_t  bufferIdZ;                     // 输出Z的buffer索引 (0-15)
    int32_t  input0Shape0;                  // input0Shape[0] (行数)
    int32_t  input0Shape1;                  // input0Shape[1] (列数)
    int32_t  input1Shape0;                  // input1Shape[0] (行数，自动=input0Shape1)
    int32_t  input1Shape1;                  // input1Shape[1] (列数)
    int8_t   shiftLeft_A;                   // 输入A左移位数
    int8_t   shiftLeft_B;                   // 输入B左移位数
    // 总共: 4+1+1+1+1+1+4+4+4+4+4+4+4+1+1 = 40字节
} __attribute__((packed)) InstructionStruct;

// 全局状态
static struct {
    // RPMsg 状态
    int ctrl_fd;       // 控制设备文件描述符
    int ept_fd;        // 端点设备文件描述符
    int log_ept_fd;    // 日志端点设备文件描述符
    bool rpmsg_initialized;

    // 共享内存状态
    int mem_fd;                         // /dev/mem 文件描述符
    void* mmap_addrs[MAX_SHM_BLOCKS];   // mmap 后的虚拟地址
    bool shm_initialized;
    pthread_mutex_t mutex;
} g_state = {
    .ctrl_fd = -1,
    .ept_fd = -1,
    .log_ept_fd = -1,
    .rpmsg_initialized = false,
    .mem_fd = -1,
    .mmap_addrs = {NULL},
    .shm_initialized = false,
    .mutex = PTHREAD_MUTEX_INITIALIZER
};

// ========== 缓冲区状态定义 ==========

// 缓冲区状态标志位定义（需与 R5 侧保持一致）
#define BUFFER_STATUS_FREE     0x00
#define BUFFER_STATUS_READY    0x01  // A53已写入数据，等待R5处理
#define BUFFER_STATUS_BUSY     0x02  // R5正在处理
#define BUFFER_STATUS_DONE     0x03  // R5处理完成
#define BUFFER_STATUS_ERROR    0xFF

// ========== 函数前向声明 ==========

static void dcache_flush(void* addr, size_t size);
static void dcache_invalidate(void* addr, size_t size);
static volatile uint32_t* get_buffer_status_addr(int bufferId);

// ========== 日志读取线程 ==========

#define LOG_BUFFER_SIZE 4096

static void* rpmsg_log_reader(void* arg) {
    (void)arg;
    char buffer[LOG_BUFFER_SIZE];
    fd_set readfds;
    struct timeval tv;

    printf("[JNI] Log reader thread started (fd=%d)\n", g_state.log_ept_fd);

    while (g_state.log_ept_fd >= 0) {
        FD_ZERO(&readfds);
        FD_SET(g_state.log_ept_fd, &readfds);
        tv.tv_sec = 1;
        tv.tv_usec = 0;

        int ret = select(g_state.log_ept_fd + 1, &readfds, NULL, NULL, &tv);
        if (ret < 0) {
            if (errno == EINTR) continue;
            perror("[JNI] Log reader select error");
            break;
        }

        if (ret > 0 && FD_ISSET(g_state.log_ept_fd, &readfds)) {
            ssize_t n = read(g_state.log_ept_fd, buffer, sizeof(buffer) - 1);
            if (n > 0) {
                buffer[n] = '\0';
                printf("%s", buffer);  // 直接输出到stdout
                fflush(stdout);
            } else if (n < 0 && errno != EAGAIN) {
                perror("[JNI] Log reader read error");
                break;
            }
        }
    }

    printf("[JNI] Log reader thread exiting\n");
    return NULL;
}

// ========== RPMsg 辅助函数 ==========

static int rpmsg_create_ept(int rpfd, struct rpmsg_endpoint_info *eptinfo) {
    return ioctl(rpfd, RPMSG_CREATE_EPT_IOCTL, eptinfo);
}

static char* get_rpmsg_ept_dev_name(const char* rpmsg_char_name,
                                     const char* ept_name,
                                     char* ept_dev_name) {
    char sys_rpmsg_ept_name_path[128];
    char svc_name[64];
    char *sys_rpmsg_path = "/sys/class/rpmsg";
    FILE *fp;
    int i;
    int ept_name_len;

    for (i = 0; i < 128; i++) {
        sprintf(sys_rpmsg_ept_name_path, "%s/%s/rpmsg%d/name",
                sys_rpmsg_path, rpmsg_char_name, i);
        if (access(sys_rpmsg_ept_name_path, F_OK) < 0)
            continue;
        fp = fopen(sys_rpmsg_ept_name_path, "r");
        if (!fp) continue;
        fgets(svc_name, sizeof(svc_name), fp);
        fclose(fp);
        ept_name_len = strlen(ept_name);
        if (ept_name_len > sizeof(svc_name))
            ept_name_len = sizeof(svc_name);
        if (!strncmp(svc_name, ept_name, ept_name_len)) {
            sprintf(ept_dev_name, "rpmsg%d", i);
            return ept_dev_name;
        }
    }
    return NULL;
}

// ========== JNI 辅助函数 ==========

static jfieldID getFieldID(JNIEnv* env, jobject obj, const char* fieldName, const char* signature) {
    jclass clazz = env->GetObjectClass(obj);
    if (clazz == NULL) return NULL;
    return env->GetFieldID(clazz, fieldName, signature);
}

static jint getIntField(JNIEnv* env, jobject obj, const char* fieldName) {
    jfieldID fid = getFieldID(env, obj, fieldName, "I");
    if (fid == NULL) return 0;
    return env->GetIntField(obj, fid);
}

static jboolean getBooleanField(JNIEnv* env, jobject obj, const char* fieldName) {
    jfieldID fid = getFieldID(env, obj, fieldName, "Z");
    if (fid == NULL) return JNI_FALSE;
    return env->GetBooleanField(obj, fid);
}

static jstring getStringField(JNIEnv* env, jobject obj, const char* fieldName) {
    jfieldID fid = getFieldID(env, obj, fieldName, "Ljava/lang/String;");
    if (fid == NULL) return NULL;
    return (jstring)env->GetObjectField(obj, fid);
}

static const char* getStringUTFChars(JNIEnv* env, jstring str) {
    if (str == NULL) return "";
    return env->GetStringUTFChars(str, NULL);
}

static void releaseStringUTFChars(JNIEnv* env, jstring str, const char* cstr) {
    if (str != NULL && cstr != NULL) {
        env->ReleaseStringUTFChars(str, cstr);
    }
}

/**
 * 将 InstJavaTODO 对象转换为本地结构
 * 与 Accelerator/InstJavaTODO.java 完全对齐
 */
static bool convertInstruction(JNIEnv* env, jobject instJava, InstructionStruct* instNative) {
    if (instJava == NULL || instNative == NULL) return false;

    memset(instNative, 0, sizeof(InstructionStruct));

    instNative->UID = getIntField(env, instJava, "UID");

    // 转换 matrixOperation (String → uint8_t)
    jstring matOp = getStringField(env, instJava, "matrixOperation");
    const char* matOpStr = getStringUTFChars(env, matOp);
    if (strcmp(matOpStr, "matmul") == 0) {
        instNative->matrixOperation = 0;
    } else if (strcmp(matOpStr, "elementadd") == 0) {
        instNative->matrixOperation = 1;
    } else if (strcmp(matOpStr, "elementmul") == 0) {
        instNative->matrixOperation = 2;
    } else if (strcmp(matOpStr, "elementmax") == 0) {
        instNative->matrixOperation = 3;
    } else {
        instNative->matrixOperation = 0;
    }
    releaseStringUTFChars(env, matOp, matOpStr);

    instNative->shiftLeft_AfterMatrixOperation = (int8_t)getIntField(env, instJava, "shiftLeft_AfterMatrixOperation");
    instNative->doTranspose = (uint8_t)getBooleanField(env, instJava, "doTranspose");

    // 转换 activationFunction (String → uint8_t)
    jstring actFunc = getStringField(env, instJava, "activationFunction");
    const char* actFuncStr = getStringUTFChars(env, actFunc);
    if (strcmp(actFuncStr, "exp") == 0) {
        instNative->activationFunction = 0;
    } else if (strcmp(actFuncStr, "log") == 0) {
        instNative->activationFunction = 1;
    } else if (strcmp(actFuncStr, "softplus") == 0) {
        instNative->activationFunction = 2;
    } else if (strcmp(actFuncStr, "relu") == 0) {
        instNative->activationFunction = 3;
    } else {
        instNative->activationFunction = 4;  // None
    }
    releaseStringUTFChars(env, actFunc, actFuncStr);

    instNative->shiftLeft_AfterActivation = (int8_t)getIntField(env, instJava, "shiftLeft_AfterActivation");

    // Buffer索引字段 (预分配池方案)
    instNative->bufferIdA = getIntField(env, instJava, "bufferIdA");
    instNative->bufferIdB = getIntField(env, instJava, "bufferIdB");
    instNative->bufferIdZ = getIntField(env, instJava, "bufferIdZ");

    // 形状字段
    instNative->input0Shape0 = getIntField(env, instJava, "input0Shape0");
    instNative->input0Shape1 = getIntField(env, instJava, "input0Shape1");
    // input1Shape0 (ROWB) - 对于 ElementWise 操作等于 input0Shape0，对于 MatMul 等于 input0Shape1
    // 检查操作类型来判断
    uint8_t matOp = instNative->matrixOperation;
    if (matOp == 1 || matOp == 2 || matOp == 3) {  // ElementWise: Add, Mul, Max
        instNative->input1Shape0 = instNative->input0Shape0;  // 形状相同
    } else {  // MatMul
        instNative->input1Shape0 = instNative->input0Shape1;  // ROWB = COLA
    }
    instNative->input1Shape1 = getIntField(env, instJava, "input1Shape1");

    // 移位字段
    instNative->shiftLeft_A = (int8_t)getIntField(env, instJava, "shiftLeft_A");
    instNative->shiftLeft_B = (int8_t)getIntField(env, instJava, "shiftLeft_B");

    return true;
}

// ========== 缓冲区状态管理函数 ==========

/**
 * 标记缓冲区状态（通过共享内存标志位）
 * @param bufferId 缓冲区ID (0-63)
 * @param status 状态: FREE, READY, BUSY, DONE
 */
static bool set_buffer_status(int bufferId, uint32_t status) {
    volatile uint32_t* status_addr = get_buffer_status_addr(bufferId);
    if (status_addr == NULL) {
        return false;
    }
    *status_addr = status;
    // 刷新缓存确保R5能看到状态更新
    dcache_flush((void*)status_addr, sizeof(uint32_t));
    printf("[JNI] Buffer %d status set to %u\n", bufferId, status);
    return true;
}

/**
 * 获取缓冲区状态
 */
static uint32_t get_buffer_status(int bufferId) {
    volatile uint32_t* status_addr = get_buffer_status_addr(bufferId);
    if (status_addr == NULL) {
        return BUFFER_STATUS_ERROR;
    }
    // 失效缓存确保读取最新状态
    dcache_invalidate((void*)status_addr, sizeof(uint32_t));
    return *status_addr;
}

/**
 * 等待缓冲区状态变更（带超时）
 * @param bufferId 缓冲区ID
 * @param expected 期望的状态
 * @param timeout_ms 超时时间（毫秒）
 * @return true=状态匹配，false=超时或错误
 */
static bool wait_for_buffer_status(int bufferId, uint32_t expected, int timeout_ms) {
    int elapsed = 0;
    const int interval_ms = 1;  // 1ms检查一次

    while (elapsed < timeout_ms) {
        uint32_t status = get_buffer_status(bufferId);
        if (status == expected) {
            printf("[JNI] Buffer %d reached expected status %u\n", bufferId, expected);
            return true;
        }
        if (status == BUFFER_STATUS_ERROR) {
            fprintf(stderr, "[JNI] Buffer %d in error state\n", bufferId);
            return false;
        }
        usleep(interval_ms * 1000);
        elapsed += interval_ms;
    }

    uint32_t final_status = get_buffer_status(bufferId);
    fprintf(stderr, "[JNI] Buffer %d status timeout: expected=%u, actual=%u\n",
            bufferId, expected, final_status);
    return false;
}

// ========== JNI 方法实现 ==========

extern "C" JNIEXPORT jboolean JNICALL
Java_org_forwarder_backend_impls_HWAccelerated_utils_HWAcceleratorJNI_nativeInitialize(
    JNIEnv* env, jobject thiz, jstring rpmsgDevice) {

    pthread_mutex_lock(&g_state.mutex);

    if (g_state.rpmsg_initialized) {
        pthread_mutex_unlock(&g_state.mutex);
        return JNI_TRUE;
    }

    const char* rpmsg_dev = env->GetStringUTFChars(rpmsgDevice, NULL);
    char fpath[256];
    char rpmsg_char_name[16];
    const char *RPMSG_BUS_SYS = "/sys/bus/rpmsg";

    printf("[JNI] Initializing RPMsg: %s\n", rpmsg_dev);

    // 检查设备是否存在
    sprintf(fpath, "%s/devices/%s", RPMSG_BUS_SYS, rpmsg_dev);
    if (access(fpath, F_OK)) {
        fprintf(stderr, "[JNI] RPMsg device not found: %s\n", fpath);
        env->ReleaseStringUTFChars(rpmsgDevice, rpmsg_dev);
        pthread_mutex_unlock(&g_state.mutex);
        return JNI_FALSE;
    }

    // 查找控制设备
    DIR *dir;
    struct dirent *ent;
    sprintf(fpath, "%s/devices/%s/rpmsg", RPMSG_BUS_SYS, rpmsg_dev);
    dir = opendir(fpath);
    if (dir == NULL) {
        fprintf(stderr, "[JNI] Failed to open rpmsg directory\n");
        env->ReleaseStringUTFChars(rpmsgDevice, rpmsg_dev);
        pthread_mutex_unlock(&g_state.mutex);
        return JNI_FALSE;
    }

    bool found = false;
    while ((ent = readdir(dir)) != NULL) {
        if (!strncmp(ent->d_name, "rpmsg_ctrl", strlen("rpmsg_ctrl"))) {
            sprintf(fpath, "/dev/%s", ent->d_name);
            g_state.ctrl_fd = open(fpath, O_RDWR | O_NONBLOCK);
            if (g_state.ctrl_fd < 0) {
                fprintf(stderr, "[JNI] Failed to open rpmsg ctrl device\n");
                closedir(dir);
                env->ReleaseStringUTFChars(rpmsgDevice, rpmsg_dev);
                pthread_mutex_unlock(&g_state.mutex);
                return JNI_FALSE;
            }
            sprintf(rpmsg_char_name, "%s", ent->d_name);
            found = true;
            break;
        }
    }
    closedir(dir);

    if (!found) {
        fprintf(stderr, "[JNI] rpmsg_ctrl device not found\n");
        env->ReleaseStringUTFChars(rpmsgDevice, rpmsg_dev);
        pthread_mutex_unlock(&g_state.mutex);
        return JNI_FALSE;
    }

    // 创建端点
    struct rpmsg_endpoint_info eptinfo;
    strcpy(eptinfo.name, "rpmsg-openamp-demo-channel");
    eptinfo.src = 0;
    eptinfo.dst = 0x400;

    if (rpmsg_create_ept(g_state.ctrl_fd, &eptinfo) != 0) {
        fprintf(stderr, "[JNI] Failed to create RPMsg endpoint\n");
        close(g_state.ctrl_fd);
        g_state.ctrl_fd = -1;
        env->ReleaseStringUTFChars(rpmsgDevice, rpmsg_dev);
        pthread_mutex_unlock(&g_state.mutex);
        return JNI_FALSE;
    }

    // 获取端点设备路径
    char ept_dev_name[16];
    if (!get_rpmsg_ept_dev_name(rpmsg_char_name, eptinfo.name, ept_dev_name)) {
        fprintf(stderr, "[JNI] Failed to get endpoint device name\n");
        close(g_state.ctrl_fd);
        g_state.ctrl_fd = -1;
        env->ReleaseStringUTFChars(rpmsgDevice, rpmsg_dev);
        pthread_mutex_unlock(&g_state.mutex);
        return JNI_FALSE;
    }

    sprintf(fpath, "/dev/%s", ept_dev_name);
    g_state.ept_fd = open(fpath, O_RDWR | O_NONBLOCK);
    if (g_state.ept_fd < 0) {
        perror("[JNI] Failed to open rpmsg endpoint device");
        close(g_state.ctrl_fd);
        g_state.ctrl_fd = -1;
        env->ReleaseStringUTFChars(rpmsgDevice, rpmsg_dev);
        pthread_mutex_unlock(&g_state.mutex);
        return JNI_FALSE;
    }

    // ========== 创建日志端点 ==========
    struct rpmsg_endpoint_info log_eptinfo;
    strcpy(log_eptinfo.name, "rpmsg-log-channel");
    log_eptinfo.src = 0;
    log_eptinfo.dst = 0x401;  // R5侧日志端点地址

    if (rpmsg_create_ept(g_state.ctrl_fd, &log_eptinfo) != 0) {
        fprintf(stderr, "[JNI] Warning: Failed to create log endpoint, R5 logs may not be visible\n");
    } else {
        // 查找日志端点设备路径
        char log_ept_dev_name[16];
        if (get_rpmsg_ept_dev_name(rpmsg_char_name, log_eptinfo.name, log_ept_dev_name)) {
            sprintf(fpath, "/dev/%s", log_ept_dev_name);
            g_state.log_ept_fd = open(fpath, O_RDWR | O_NONBLOCK);
            if (g_state.log_ept_fd >= 0) {
                printf("[JNI] Log endpoint opened: %s (fd=%d)\n", fpath, g_state.log_ept_fd);

                // 启动日志读取线程
                pthread_t log_tid;
                if (pthread_create(&log_tid, NULL, rpmsg_log_reader, NULL) == 0) {
                    pthread_detach(log_tid);
                    printf("[JNI] Log reader thread started\n");
                } else {
                    perror("[JNI] Failed to create log reader thread");
                    close(g_state.log_ept_fd);
                    g_state.log_ept_fd = -1;
                }
            } else {
                perror("[JNI] Failed to open log endpoint device");
            }
        }
    }

    g_state.rpmsg_initialized = true;  // 【修复】initialized → rpmsg_initialized
    env->ReleaseStringUTFChars(rpmsgDevice, rpmsg_dev);

    printf("[JNI] RPMsg initialized: ctrl=%d, ept=%d\n", g_state.ctrl_fd, g_state.ept_fd);
    pthread_mutex_unlock(&g_state.mutex);
    return JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL
Java_org_forwarder_backend_impls_HWAccelerated_utils_HWAcceleratorJNI_nativeShutdown(
    JNIEnv* env, jobject thiz) {

    pthread_mutex_lock(&g_state.mutex);

    if (g_state.rpmsg_initialized) {
        // 关闭日志端点
        if (g_state.log_ept_fd >= 0) {
            close(g_state.log_ept_fd);
            g_state.log_ept_fd = -1;
        }
        if (g_state.ept_fd >= 0) {
            close(g_state.ept_fd);
            g_state.ept_fd = -1;
        }
        if (g_state.ctrl_fd >= 0) {
            close(g_state.ctrl_fd);
            g_state.ctrl_fd = -1;
        }
        g_state.rpmsg_initialized = false;  // 【修复】initialized → rpmsg_initialized
    }

    pthread_mutex_unlock(&g_state.mutex);
    printf("[JNI] RPMsg shutdown\n");
}

extern "C" JNIEXPORT jboolean JNICALL
Java_org_forwarder_backend_impls_HWAccelerated_utils_HWAcceleratorJNI_nativeSendInstructions(
    JNIEnv* env, jobject thiz, jobjectArray instructions, jint count) {

    if (!g_state.rpmsg_initialized || g_state.ept_fd < 0) {  // 【修复】initialized → rpmsg_initialized
        return JNI_FALSE;
    }

    pthread_mutex_lock(&g_state.mutex);

    jint actualCount = (count < MAX_INSTRUCTIONS) ? count : MAX_INSTRUCTIONS;

    // RPMsg 缓冲区大小通常为 512B，可以发送多条指令
    // 计算数据包大小
    size_t data_size = sizeof(rpmsg_header_t) + actualCount * sizeof(InstructionStruct);

    // 分配缓冲区
    uint8_t* buffer = (uint8_t*)malloc(data_size);
    if (buffer == NULL) {
        pthread_mutex_unlock(&g_state.mutex);
        return JNI_FALSE;
    }

    // 填充消息头
    rpmsg_header_t* header = (rpmsg_header_t*)buffer;
    header->cmd = RPMSG_CMD_INSTRUCTIONS_DATA;
    header->count = actualCount;

    // 填充指令数据
    InstructionStruct* instArray = (InstructionStruct*)(buffer + sizeof(rpmsg_header_t));
    for (jint i = 0; i < actualCount; i++) {
        jobject instJava = env->GetObjectArrayElement(instructions, i);
        if (instJava == NULL) {
            header->count = i;  // 更新实际数量
            break;
        }
        convertInstruction(env, instJava, &instArray[i]);

        // 设置输入缓冲区状态为 READY（标记数据已准备好）
        int bufferIdA = instArray[i].bufferIdA;
        int bufferIdB = instArray[i].bufferIdB;
        int bufferIdZ = instArray[i].bufferIdZ;

        set_buffer_status(bufferIdA, BUFFER_STATUS_READY);
        set_buffer_status(bufferIdB, BUFFER_STATUS_READY);
        set_buffer_status(bufferIdZ, BUFFER_STATUS_FREE);  // 输出缓冲区初始为FREE

        env->DeleteLocalRef(instJava);
    }

    // 通过 RPMsg 发送（内核驱动自动处理 vring 更新和 IPI 触发）
    ssize_t sent = write(g_state.ept_fd, buffer, sizeof(rpmsg_header_t) + header->count * sizeof(InstructionStruct));
    free(buffer);

    if (sent < 0) {
        fprintf(stderr, "[JNI] RPMsg send failed: %s\n", strerror(errno));
        pthread_mutex_unlock(&g_state.mutex);
        return JNI_FALSE;
    }

    printf("[JNI] Sent %u instructions via RPMsg (%zd bytes)\n", header->count, sent);
    pthread_mutex_unlock(&g_state.mutex);
    return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_org_forwarder_backend_impls_HWAccelerated_utils_HWAcceleratorJNI_nativeWaitForCompletion(
    JNIEnv* env, jobject thiz, jint timeoutMs) {

    if (!g_state.rpmsg_initialized || g_state.ept_fd < 0) {  // 【修复】initialized → rpmsg_initialized
        return JNI_FALSE;
    }

    struct pollfd pfd;
    pfd.fd = g_state.ept_fd;
    pfd.events = POLLIN;
    pfd.revents = 0;

    int elapsed = 0;
    const int pollInterval = 10;  // 10ms

    while (elapsed < timeoutMs) {
        int ret = poll(&pfd, 1, pollInterval);
        if (ret < 0) {
            fprintf(stderr, "[JNI] Poll failed: %s\n", strerror(errno));
            return JNI_FALSE;
        }
        if (ret > 0 && (pfd.revents & POLLIN)) {
            // 读取响应
            uint32_t response;
            ssize_t n = read(g_state.ept_fd, &response, sizeof(response));
            if (n == sizeof(response) && response == 0x03) {  // RPMSG_CMD_COMPLETION
                printf("[JNI] Received completion notification\n");
                return JNI_TRUE;
            }
        }
        elapsed += pollInterval;
    }

    fprintf(stderr, "[JNI] Timeout waiting for completion\n");
    return JNI_FALSE;
}

/**
 * 等待指定缓冲区完成处理（基于共享内存状态标志）
 * 替代轮询 RPMsg 的方式，直接检查缓冲区状态
 */
extern "C" JNIEXPORT jboolean JNICALL
Java_org_forwarder_backend_impls_HWAccelerated_utils_HWAcceleratorJNI_nativeWaitForBufferCompletion(
    JNIEnv* env, jobject thiz, jint bufferId, jint timeoutMs) {

    if (bufferId < 0 || bufferId >= 64) {
        fprintf(stderr, "[JNI] Invalid bufferId: %d\n", bufferId);
        return JNI_FALSE;
    }

    printf("[JNI] Waiting for buffer %d completion...\n", bufferId);
    bool success = wait_for_buffer_status(bufferId, BUFFER_STATUS_DONE, timeoutMs);

    if (success) {
        // 处理完成后，将缓冲区状态设置为 FREE
        set_buffer_status(bufferId, BUFFER_STATUS_FREE);
    }

    return success ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jint JNICALL
Java_org_forwarder_backend_impls_HWAccelerated_utils_HWAcceleratorJNI_nativeGetStatus(
    JNIEnv* env, jobject thiz) {
    // 简单实现：检查是否有活跃的 buffer
    // 更精确的实现需要通过 RPMsg 查询 R5 侧状态
    if (!g_state.rpmsg_initialized) {
        return FPGA_STATUS_ERROR;
    }
    return FPGA_STATUS_IDLE;  // 假设空闲，实际应查询硬件
}

extern "C" JNIEXPORT jboolean JNICALL
Java_org_forwarder_backend_impls_HWAccelerated_utils_HWAcceleratorJNI_nativeWriteDDR(
    JNIEnv* env, jobject thiz, jlong address, jbyteArray data, jint length) {
    // DDR 写入已通过共享内存池实现
    // 此函数保留用于特殊情况下的直接 DDR 访问
    if (!g_state.shm_initialized || data == NULL) {
        return JNI_FALSE;
    }

    // 计算目标地址所在的 block 和 offset
    for (int i = 0; i < MAX_SHM_BLOCKS; i++) {
        unsigned long block_start = SHM_CONFIG[i].phys_addr;
        unsigned long block_end = block_start + SHM_CONFIG[i].size;

        if ((unsigned long)address >= block_start && (unsigned long)address < block_end) {
            // 目标地址在此共享内存块中
            void* dst = (char*)g_state.mmap_addrs[i] + (address - block_start);

            jbyte* src = env->GetByteArrayElements(data, NULL);
            if (src == NULL) {
                return JNI_FALSE;
            }

            memcpy(dst, src, length);
            env->ReleaseByteArrayElements(data, src, 0);

            // 刷新缓存
            dcache_flush(dst, length);

            printf("[JNI] Written %d bytes to DDR at 0x%lx\n", length, address);
            return JNI_TRUE;
        }
    }

    fprintf(stderr, "[JNI] nativeWriteDDR: address 0x%lx not in shared memory range\n", address);
    return JNI_FALSE;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_org_forwarder_backend_impls_HWAccelerated_utils_HWAcceleratorJNI_nativeReadDDR(
    JNIEnv* env, jobject thiz, jlong address, jint length) {
    // DDR 读取已通过共享内存池实现
    // 此函数保留用于特殊情况下的直接 DDR 访问
    if (!g_state.shm_initialized || length <= 0) {
        return NULL;
    }

    // 计算目标地址所在的 block 和 offset
    for (int i = 0; i < MAX_SHM_BLOCKS; i++) {
        unsigned long block_start = SHM_CONFIG[i].phys_addr;
        unsigned long block_end = block_start + SHM_CONFIG[i].size;

        if ((unsigned long)address >= block_start && (unsigned long)address < block_end) {
            if ((unsigned long)address + length > block_end) {
                fprintf(stderr, "[JNI] nativeReadDDR: read exceeds block boundary\n");
                return NULL;
            }

            // 失效缓存
            void* src = (char*)g_state.mmap_addrs[i] + (address - block_start);
            dcache_invalidate(src, length);

            // 创建 Java 字节数组
            jbyteArray result = env->NewByteArray(length);
            if (result == NULL) {
                return NULL;
            }

            env->SetByteArrayRegion(result, 0, length, (jbyte*)src);

            printf("[JNI] Read %d bytes from DDR at 0x%lx\n", length, address);
            return result;
        }
    }

    fprintf(stderr, "[JNI] nativeReadDDR: address 0x%lx not in shared memory range\n", address);
    return NULL;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_org_forwarder_backend_impls_HWAccelerated_utils_HWAcceleratorJNI_nativeReadResult(
    JNIEnv* env, jobject thiz, jint bufferId, jint length) {
    // 从指定的 buffer 读取计算结果
    if (!g_state.shm_initialized || bufferId < 0 || bufferId >= 64) {
        return NULL;
    }

    // 计算 buffer 位置
    int blockId = bufferId / 16;
    int bufferInBlock = bufferId % 16;
    size_t buffer_offset = bufferInBlock * (640 * 1024) + 64;  // +64 跳过状态标志

    if (blockId >= MAX_SHM_BLOCKS || g_state.mmap_addrs[blockId] == NULL) {
        return NULL;
    }

    if (length <= 0 || length > (640 * 1024 - 64)) {
        length = 640 * 1024 - 64;  // 默认读取整个 buffer
    }

    // 失效缓存
    void* src = (char*)g_state.mmap_addrs[blockId] + buffer_offset;
    dcache_invalidate(src, length);

    // 创建 Java 字节数组
    jbyteArray result = env->NewByteArray(length);
    if (result == NULL) {
        return NULL;
    }

    env->SetByteArrayRegion(result, 0, length, (jbyte*)src);

    printf("[JNI] Read %d bytes from buffer %d\n", length, bufferId);
    return result;
}

// ========== ARM Cache 操作辅助函数 ==========

/**
 * ARM 数据缓存操作 - 刷新到内存
 * 确保A53写入的数据对R5/DataMover可见
 */
static void dcache_flush(void* addr, size_t size) {
    unsigned long start = (unsigned long)addr;
    unsigned long end = start + size;
    unsigned long line_size = 64;  // ARMv8 cache line size

    // 按cache line对齐
    start &= ~(line_size - 1);
    end = (end + line_size - 1) & ~(line_size - 1);

    for (unsigned long line = start; line < end; line += line_size) {
        __asm__ volatile(
            "dc cvac, %0"  // Clean to Point of Coherency
            :
            : "r"(line)
            : "memory"
        );
    }

    // 数据同步屏障
    __asm__ volatile("dmb sy" ::: "memory");
}

/**
 * ARM 数据缓存操作 - 失效
 * 确保读取R5/DataMover写入的最新数据
 */
static void dcache_invalidate(void* addr, size_t size) {
    unsigned long start = (unsigned long)addr;
    unsigned long end = start + size;
    unsigned long line_size = 64;

    // 按cache line对齐
    start &= ~(line_size - 1);
    end = (end + line_size - 1) & ~(line_size - 1);

    for (unsigned long line = start; line < end; line += line_size) {
        __asm__ volatile(
            "dc ivac, %0"  // Invalidate to Point of Coherency
            :
            : "r"(line)
            : "memory"
        );
    }

    // 数据同步屏障
    __asm__ volatile("dmb sy" ::: "memory");
}

// ========== 缓冲区状态管理（通过共享内存标志位同步） ==========

/**
 * 获取缓冲区状态标志位的地址
 * 标志位位于每个buffer的起始位置，使用前4字节存储状态
 */
static volatile uint32_t* get_buffer_status_addr(int bufferId) {
    if (bufferId < 0 || bufferId >= 64) return NULL;

    // 每个block有16个buffer，每个640KB
    int blockId = bufferId / 16;
    int bufferInBlock = bufferId % 16;

    if (blockId >= MAX_SHM_BLOCKS || g_state.mmap_addrs[blockId] == NULL) {
        return NULL;
    }

    // 计算buffer在block中的偏移
    size_t buffer_offset = bufferInBlock * (640 * 1024);
    return (volatile uint32_t*)((char*)g_state.mmap_addrs[blockId] + buffer_offset);
}

// ========== 共享内存相关 JNI 方法 ==========

extern "C" JNIEXPORT jboolean JNICALL
Java_org_forwarder_backend_impls_HWAccelerated_utils_HWAcceleratorJNI_nativeInitializeSharedMemory(
    JNIEnv* env, jobject thiz, jlongArray physAddresses, jintArray sizes) {

    pthread_mutex_lock(&g_state.mutex);

    if (g_state.shm_initialized) {
        pthread_mutex_unlock(&g_state.mutex);
        return JNI_TRUE;
    }

    printf("[JNI] Initializing shared memory pool...\n");

    // 打开 /dev/mem
    g_state.mem_fd = open(MEM_DEV, O_RDWR | O_SYNC);
    if (g_state.mem_fd < 0) {
        fprintf(stderr, "[JNI] Failed to open %s: %s\n", MEM_DEV, strerror(errno));
        pthread_mutex_unlock(&g_state.mutex);
        return JNI_FALSE;
    }

    // 映射每个内存块
    for (int i = 0; i < MAX_SHM_BLOCKS; i++) {
        void* mapped = mmap(NULL, SHM_CONFIG[i].size,
                            PROT_READ | PROT_WRITE, MAP_SHARED,
                            g_state.mem_fd, SHM_CONFIG[i].phys_addr);

        if (mapped == MAP_FAILED) {
            fprintf(stderr, "[JNI] Failed to mmap block %d at 0x%lx: %s\n",
                    i, SHM_CONFIG[i].phys_addr, strerror(errno));

            // 清理已映射的块
            for (int j = 0; j < i; j++) {
                munmap(g_state.mmap_addrs[j], SHM_CONFIG[j].size);
                g_state.mmap_addrs[j] = NULL;
            }
            close(g_state.mem_fd);
            g_state.mem_fd = -1;
            pthread_mutex_unlock(&g_state.mutex);
            return JNI_FALSE;
        }

        g_state.mmap_addrs[i] = mapped;
        printf("[JNI] Mapped block %d: PA=0x%lx, VA=%p, Size=%zuMB\n",
               i, SHM_CONFIG[i].phys_addr, mapped,
               SHM_CONFIG[i].size / (1024 * 1024));
    }

    g_state.shm_initialized = true;
    printf("[JNI] Shared memory pool initialized successfully\n");
    pthread_mutex_unlock(&g_state.mutex);
    return JNI_TRUE;
}

extern "C" JNIEXPORT jlong JNICALL
Java_org_forwarder_backend_impls_HWAccelerated_utils_HWAcceleratorJNI_nativeMapSharedMemoryBlock(
    JNIEnv* env, jobject thiz, jint blockId, jint size) {

    if (!g_state.shm_initialized) {
        fprintf(stderr, "[JNI] Shared memory not initialized\n");
        return 0;
    }

    if (blockId < 0 || blockId >= MAX_SHM_BLOCKS) {
        fprintf(stderr, "[JNI] Invalid block ID: %d\n", blockId);
        return 0;
    }

    if (g_state.mmap_addrs[blockId] == NULL) {
        fprintf(stderr, "[JNI] Block %d not mapped\n", blockId);
        return 0;
    }

    printf("[JNI] Mapping block %d: returning VA=%p\n", blockId, g_state.mmap_addrs[blockId]);
    return (jlong)(intptr_t)g_state.mmap_addrs[blockId];
}

extern "C" JNIEXPORT void JNICALL
Java_org_forwarder_backend_impls_HWAccelerated_utils_HWAcceleratorJNI_nativeUnmapSharedMemoryBlock(
    JNIEnv* env, jobject thiz, jint blockId, jlong mmapAddr) {
    // 不需要取消映射，因为整个块在初始化时已经映射
    printf("[JNI] Unmap requested for block %d (no-op)\n", blockId);
}

/**
 * 同步数据到设备（A53 → R5/DataMover）
 * 1. 刷新数据缓存
 * 2. 设置缓冲区状态为 READY
 */
extern "C" JNIEXPORT void JNICALL
Java_org_forwarder_backend_impls_HWAccelerated_utils_HWAcceleratorJNI_nativeSyncSharedMemoryToDevice(
    JNIEnv* env, jobject thiz, jint blockId, jint offset, jint size) {

    if (!g_state.shm_initialized || blockId < 0 || blockId >= MAX_SHM_BLOCKS) {
        return;
    }

    void* addr = (char*)g_state.mmap_addrs[blockId] + offset;

    // 刷新数据缓存到内存（确保R5/DataMover能看到最新数据）
    dcache_flush(addr, size);

    printf("[JNI] Synced to device: block=%d, offset=0x%x, size=%d (cache flushed)\n",
           blockId, offset, size);
}

/**
 * 从设备同步数据（R5/DataMover → A53）
 * 1. 失效数据缓存
 * 2. 设置缓冲区状态为 FREE
 */
/**
 * 从设备同步数据（R5/DataMover → A53）
 * 1. 失效数据缓存
 * 2. 内存屏障确保设备写入对CPU可见
 */
extern "C" JNIEXPORT void JNICALL
Java_org_forwarder_backend_impls_HWAccelerated_utils_HWAcceleratorJNI_nativeSyncSharedMemoryFromDevice(
    JNIEnv* env, jobject thiz, jint blockId, jint offset, jint size) {

    if (!g_state.shm_initialized || blockId < 0 || blockId >= MAX_SHM_BLOCKS) {
        return;
    }

    void* addr = (char*)g_state.mmap_addrs[blockId] + offset;

    // 失效数据缓存（强制从内存重新读取R5/DataMover写入的数据）
    dcache_invalidate(addr, size);

    // 内存屏障，使设备写入对CPU可见
    __sync_synchronize();

    printf("[JNI] Synced from device: block=%d, offset=0x%x, size=%d (cache invalidated)\n",
           blockId, offset, size);
}

extern "C" JNIEXPORT void JNICALL
Java_org_forwarder_backend_impls_HWAccelerated_utils_HWAcceleratorJNI_nativeShutdownSharedMemory(
    JNIEnv* env, jobject thiz) {

    pthread_mutex_lock(&g_state.mutex);

    if (!g_state.shm_initialized) {
        pthread_mutex_unlock(&g_state.mutex);
        return;
    }

    printf("[JNI] Shutting down shared memory...\n");

    // 取消映射所有块
    for (int i = 0; i < MAX_SHM_BLOCKS; i++) {
        if (g_state.mmap_addrs[i] != NULL) {
            munmap(g_state.mmap_addrs[i], SHM_CONFIG[i].size);
            g_state.mmap_addrs[i] = NULL;
        }
    }

    // 关闭 /dev/mem
    if (g_state.mem_fd >= 0) {
        close(g_state.mem_fd);
        g_state.mem_fd = -1;
    }

    g_state.shm_initialized = false;
    printf("[JNI] Shared memory shutdown complete\n");
    pthread_mutex_unlock(&g_state.mutex);
}
