/*
 * accelerator_jni.cpp
 *
 * JNI bridge between Java and R5/FPGA accelerator using OpenAMP RPMsg
 *
 * Communication flow:
 * - Java → JNI → write(/dev/rpmsgX) → kernel driver → vring + IPI → R5
 * - No need for application-layer shared memory mapping
 *
 * Compile: gcc -shared -fPIC -o libaccelerator_jni.so \
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
#include <linux/rpmsg.h>
#include <dirent.h>

#include "rpmsg_comm.h"

// RPMsg 消息头
typedef struct {
    uint32_t cmd;       // 命令类型
    uint32_t count;     // 指令数量
} __attribute__((packed)) rpmsg_header_t;

#define MAX_INSTRUCTIONS 256  // 单次发送最大指令数

// RPMsg 命令 (使用 rpmsg_comm.h 中的定义)
#define RPMSG_CMD_INSTRUCTIONS_DATA 0x02  // 直接发送指令数据

// InstJavaTODO 指令结构 (对应硬件 ComputeInstruction_Simplified_TypeDef)
// 根据 Interface.scala 中 ComputeInstruction_Simplified_TypeDef 定义
typedef struct {
    uint32_t UID;                           // 唯一标识符
    uint32_t matrixOperation;               // 0=MatMul, 1=ElementAdd, 2=ElementMul, 3=ElementMax
    int32_t  shiftLeft_AfterMatrixOperation;
    uint8_t  doTranspose;
    uint32_t activationFunction;            // 0=Exp, 1=Log, 2=Softplus, 3=Relu, 4=None
    int32_t  shiftLeft_AfterActivation;
    // 地址字段在 Simplified_TypeDef 中固定为 0，不需要传输
    uint32_t input0Shape0;                  // input0Shape[0]
    uint32_t input0Shape1;                  // input0Shape[1]
    uint32_t input1Shape0;                  // input1Shape[0] ← 新增！
    uint32_t input1Shape1;                  // input1Shape[1]
    int32_t  shiftLeft_A;
    int32_t  shiftLeft_B;
    // 总共: 4+4+4+1+4+4+4+4+4+4+4+4 = 45字节，对齐到48字节
    uint8_t  _padding[3];                   // 对齐到48字节
} __attribute__((packed)) InstructionStruct;

// 全局状态
// OpenAMP RPMsg 通过内核驱动管理所有共享内存和 vring 同步
static struct {
    int ctrl_fd;       // 控制设备文件描述符
    int ept_fd;        // 端点设备文件描述符
    pthread_mutex_t mutex;
    bool initialized;
} g_state = {
    .ctrl_fd = -1,
    .ept_fd = -1,
    .mutex = PTHREAD_MUTEX_INITIALIZER,
    .initialized = false
};

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

// 将 InstJavaTODO 对象转换为本地结构
static bool convertInstruction(JNIEnv* env, jobject instJava, InstructionStruct* instNative) {
    if (instJava == NULL || instNative == NULL) return false;

    memset(instNative, 0, sizeof(InstructionStruct));

    instNative->UID = getIntField(env, instJava, "UID");

    // 转换 matrixOperation
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

    instNative->shiftLeft_AfterMatrixOperation = getIntField(env, instJava, "shiftLeft_AfterMatrixOperation");
    instNative->doTranspose = getBooleanField(env, instJava, "doTranspose");

    // 转换 activationFunction
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

    instNative->shiftLeft_AfterActivation = getIntField(env, instJava, "shiftLeft_AfterActivation");
    // 地址字段不需要传输（Simplified_TypeDef 中固定为0）
    instNative->input0Shape0 = getIntField(env, instJava, "input0Shape0");
    instNative->input0Shape1 = getIntField(env, instJava, "input0Shape1");
    instNative->input1Shape0 = getIntField(env, instJava, "input1Shape0");  // 新增
    instNative->input1Shape1 = getIntField(env, instJava, "input1Shape1");
    instNative->shiftLeft_A = getIntField(env, instJava, "shiftLeft_A");
    instNative->shiftLeft_B = getIntField(env, instJava, "shiftLeft_B");

    return true;
}

// ========== JNI 方法实现 ==========

extern "C" JNIEXPORT jboolean JNICALL
Java_org_forwarder_backend_impls_HWAccelerated_utils_HWAcceleratorJNI_nativeInitialize(
    JNIEnv* env, jobject thiz, jstring rpmsgDevice) {

    pthread_mutex_lock(&g_state.mutex);

    if (g_state.initialized) {
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

    g_state.initialized = true;
    env->ReleaseStringUTFChars(rpmsgDevice, rpmsg_dev);

    printf("[JNI] RPMsg initialized: ctrl=%d, ept=%d\n", g_state.ctrl_fd, g_state.ept_fd);
    pthread_mutex_unlock(&g_state.mutex);
    return JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL
Java_org_forwarder_backend_impls_HWAccelerated_utils_HWAcceleratorJNI_nativeShutdown(
    JNIEnv* env, jobject thiz) {

    pthread_mutex_lock(&g_state.mutex);

    if (g_state.initialized) {
        if (g_state.ept_fd >= 0) {
            close(g_state.ept_fd);
            g_state.ept_fd = -1;
        }
        if (g_state.ctrl_fd >= 0) {
            close(g_state.ctrl_fd);
            g_state.ctrl_fd = -1;
        }
        g_state.initialized = false;
    }

    pthread_mutex_unlock(&g_state.mutex);
    printf("[JNI] RPMsg shutdown\n");
}

extern "C" JNIEXPORT jboolean JNICALL
Java_org_forwarder_backend_impls_HWAccelerated_utils_HWAcceleratorJNI_nativeSendInstructions(
    JNIEnv* env, jobject thiz, jobjectArray instructions, jint count) {

    if (!g_state.initialized || g_state.ept_fd < 0) {
        return JNI_FALSE;
    }

    pthread_mutex_lock(&g_state.mutex);

    jint actualCount = (count < MAX_INSTRUCTIONS) ? count : MAX_INSTRUCTIONS;

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

    if (!g_state.initialized || g_state.ept_fd < 0) {
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
            if (n == sizeof(response) && response == RPMSG_CMD_COMPLETION) {
                printf("[JNI] Received completion notification\n");
                return JNI_TRUE;
            }
        }
        elapsed += pollInterval;
    }

    fprintf(stderr, "[JNI] Timeout waiting for completion\n");
    return JNI_FALSE;
}

extern "C" JNIEXPORT jint JNICALL
Java_org_forwarder_backend_impls_HWAccelerated_utils_HWAcceleratorJNI_nativeGetStatus(
    JNIEnv* env, jobject thiz) {
    // TODO: 通过 RPMsg 查询状态
    return 0;  // STATUS_IDLE
}

extern "C" JNIEXPORT jboolean JNICALL
Java_org_forwarder_backend_impls_HWAccelerated_utils_HWAcceleratorJNI_nativeWriteDDR(
    JNIEnv* env, jobject thiz, jlong address, jbyteArray data, jint length) {
    // TODO: 通过 RPMsg 发送 DDR 写入请求
    fprintf(stderr, "[JNI] nativeWriteDDR: use RPMsg to request R5 to write DDR\n");
    return JNI_FALSE;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_org_forwarder_backend_impls_HWAccelerated_utils_HWAcceleratorJNI_nativeReadDDR(
    JNIEnv* env, jobject thiz, jlong address, jint length) {
    // TODO: 通过 RPMsg 请求 R5 读取 DDR
    fprintf(stderr, "[JNI] nativeReadDDR: use RPMsg to request R5 to read DDR\n");
    return NULL;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_org_forwarder_backend_impls_HWAccelerated_utils_HWAcceleratorJNI_nativeReadResult(
    JNIEnv* env, jobject thiz, jint offset, jint length) {
    // TODO: 读取计算结果
    return NULL;
}
