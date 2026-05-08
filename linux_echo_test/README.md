# FPGA Accelerator JNI Interface

## 目录结构

```
linux_echo_test/
├── echotest.c              # 原 RPMsg 回环测试
├── accelerator_jni.cpp      # JNI 实现 (新增)
├── rpmsg_comm.c            # RPMsg 通信封装 (新增)
├── rpmsg_comm.h            # RPMsg 通信头文件 (新增)
├── build_jni.sh            # JNI 库编译脚本 (新增)
└── libaccelerator_jni.so   # 编译输出的 JNI 库
```

## 编译 JNI 库

```bash
cd linux_echo_test
chmod +x build_jni.sh
./build_jni.sh
```

编译成功后会生成 `libaccelerator_jni.so`

## 安装 JNI 库

### 方法 1: 系统安装 (推荐)

```bash
sudo cp libaccelerator_jni.so /usr/local/lib/
sudo ldconfig
```

### 方法 2: 设置 LD_LIBRARY_PATH

```bash
export LD_LIBRARY_PATH=$(pwd):$LD_LIBRARY_PATH
```

### 方法 3: Java 启动时指定

```bash
java -Djava.library.path=/path/to/linux_echo_test -cp ... YourClass
```

## Java 端使用

### 1. 初始化

```java
import org.forwarder.backend.impls.HWAccelerated.utils.HWAcceleratorJNI;

HWAcceleratorJNI jni = HWAcceleratorJNI.getInstance();
if (!jni.initialize()) {
    System.err.println("Failed to initialize JNI");
    return;
}
```

### 2. 发送指令

```java
import Accelerator.InstJavaTODO;

InstJavaTODO[] instructions = new InstJavaTODO[]{
    new InstJavaTODO(
        0,              // UID
        "matmul",       // matrixOperation
        0,              // shiftLeft_AfterMatrixOperation
        false,          // doTranspose
        "none",         // activationFunction
        0,              // shiftLeft_AfterActivation
        0, 0, 0,        // addresses (unused in Simplified_TypeDef)
        16, 16, 16,     // shapes
        0, 0            // shiftLeft_A/B
    )
};

boolean success = jni.executeInstructions(instructions);
```

### 3. 清理

```java
jni.shutdown();
```

## RPMsg 通信

JNI 层使用 Linux RPMsg 字符设备与 R5 核通信，通过 vdev0buffer 共享内存交换数据：

**设备树内存布局 (RPU0):**
```
0x3ed40000 - 0x3ed43fff: vring0 (RPMsg)
0x3ed44000 - 0x3ed47fff: vring1 (RPMsg)
0x3ed48000 - 0x3ee47fff: vdev0buffer (共享数据) ★
```

**通信流程:**
1. A53 通过 `/dev/mem` mmap `vdev0buffer` (0x3ed48000)
2. A53 写入指令到 `vdev0buffer`
3. A53 通过 RPMsg 发送通知给 R5
4. R5 从 `vdev0buffer` 读取指令
5. R5 配置 FPGA 执行计算
6. R5 通过 RPMsg 发送完成通知
7. A53 从 `vdev0buffer` 读取结果

**重要: 需要访问 `/dev/mem`**
```bash
# 方法1: 临时开放权限 (测试用)
sudo chmod 666 /dev/mem

# 方法2: 将用户添加到特定组
sudo usermod -a -G kvm $USER  # 某些系统使用 kvm 组访问 /dev/mem

# 方法3: 使用自定义设备驱动暴露 vdev0buffer (推荐用于生产环境)
```

## 调试

### 查看 RPMsg 设备

```bash
ls -la /dev/rpmsg*
ls -la /sys/class/rpmsg/
```

### 检查共享内存

```bash
ls -la /dev/shm/
```

### 查看 JNI 日志

JNI 会打印调试信息到 stdout，确保 Java 应用运行时能看到输出。

## 依赖

- Linux kernel with RPMsg support
- rpmsg_char kernel module
- OpenAMP library (for R5 firmware)
- Java JDK/JRE 11+

## 故障排查

### JNI 库加载失败

```
java.lang.UnsatisfiedLinkError: no accelerator_jni in java.library.path
```

解决方案：确保 `libaccelerator_jni.so` 在库路径中

### RPMsg 设备打开失败

```
Failed to open RPMsg device: /dev/rpmsgX
```

解决方案：
1. 检查 R5 核是否已启动
2. 检查 rpmsg_char 驱动是否加载
3. 检查设备权限

### 共享内存创建失败

```
Failed to open /dev/mem: Permission denied
```

解决方案：
```bash
# 测试用 (临时)
sudo chmod 666 /dev/mem

# 或使用 sudo 运行 Java 程序
sudo java -jar your_app.jar
```

生产环境应创建专用字符设备驱动来安全地暴露 `vdev0buffer`。
