#!/bin/bash
#
# build_jni.sh - Build JNI library for FPGA accelerator
#
# Usage:
#   ./build_jni.sh              # Native build (x86_64)
#   ./build_jni.sh cross        # Cross compile for ARM aarch64
#
# Cross compilation requires Xilinx Vitis tools or aarch64-linux-gnu toolchain:
#   - Source Vitis settings: source /tools/Xilinx/Vitis/2023.2/settings64.sh
#   - Or install: sudo apt-get install g++-aarch64-linux-gnu
#

set -e

# 检测编译模式
BUILD_MODE=${1:-native}

# 配置
JAVA_HOME=${JAVA_HOME:-/usr/lib/jvm/java-11-openjdk-amd64}
TARGET_LIB=libaccelerator_jni.so

if [ "$BUILD_MODE" = "cross" ]; then
    # 交叉编译模式 (ARM aarch64)
    echo "========================================="
    echo "Cross compiling for ARM aarch64"
    echo "========================================="

    # 尝试自动检测 Xilinx 或系统 aarch64 工具链
    if [ -n "$XILINX_VITIS" ]; then
        # Xilinx Vitis 工具链
        CC="aarch64-xilinx-linux-gcc"
        CXX="aarch64-xilinx-linux-g++"
        echo "Using Xilinx toolchain (VITIS=$XILINX_VITIS)"
    elif command -v aarch64-linux-gnu-g++ &> /dev/null; then
        # 系统 aarch64 工具链
        CC="aarch64-linux-gnu-gcc"
        CXX="aarch64-linux-gnu-g++"
        echo "Using system aarch64-linux-gnu toolchain"
    else
        echo "ERROR: No ARM cross compiler found!"
        echo ""
        echo "Please either:"
        echo "  1. Source Xilinx Vitis settings:"
        echo "     source /tools/Xilinx/Vitis/2023.2/settings64.sh"
        echo "  2. Or install GNU cross compiler:"
        echo "     sudo apt-get install g++-aarch64-linux-gnu"
        exit 1
    fi

    # 交叉编译时可能需要指定 sysroot
    SYSROOT=${SYSROOT:-}
    if [ -n "$SYSROOT" ]; then
        CXXFLAGS="--sysroot=$SYSROOT"
    fi
else
    # 本地编译模式 (x86_64 - 仅用于测试!)
    echo "========================================="
    echo "Native build (x86_64) - FOR TESTING ONLY!"
    echo "========================================="
    echo "WARNING: This builds for x86_64, not ARM!"
    echo "Use '$0 cross' to build for target platform."
    echo ""

    CC=${CC:-gcc}
    CXX=${CXX:-g++}
fi

echo "JAVA_HOME: $JAVA_HOME"
echo "Compiler:   $CXX"
echo ""

# 检查 JAVA_HOME
if [ ! -d "$JAVA_HOME" ]; then
    echo "ERROR: JAVA_HOME not found: $JAVA_HOME"
    echo "Please set JAVA_HOME to your Java installation"
    exit 1
fi

JAVA_INCLUDE="$JAVA_HOME/include"
JAVA_INCLUDE_LINUX="$JAVA_INCLUDE/linux"

if [ ! -d "$JAVA_INCLUDE" ]; then
    echo "ERROR: Java include directory not found: $JAVA_INCLUDE"
    exit 1
fi

# 编译选项
CFLAGS="-fPIC -O2 -Wall"
CXXFLAGS="$CXXFLAGS -fPIC -O2 -Wall -std=c++11"
LDFLAGS="-shared -lpthread"

# 包含路径
INCLUDES="-I$JAVA_INCLUDE -I$JAVA_INCLUDE_LINUX"

# 源文件
SOURCES="accelerator_jni.cpp rpmsg_comm.c"

# 编译
echo "Compiling $SOURCES..."
$CXX $CXXFLAGS $INCLUDES $LDFLAGS -o $TARGET_LIB $SOURCES

if [ $? -eq 0 ]; then
    echo ""
    echo "========================================="
    echo "Build successful!"
    echo "========================================="

    # 显示生成的库信息
    if command -v file &> /dev/null; then
        echo ""
        file $TARGET_LIB
    fi
    if command -v readelf &> /dev/null; then
        echo "Machine: $(readelf -h $TARGET_LIB | grep Machine | awk '{print $2}')"
    fi

    echo ""
    echo "Output: $TARGET_LIB"
    echo ""
    if [ "$BUILD_MODE" = "cross" ]; then
        echo "Deploy to target board:"
        echo "  scp $TARGET_LIB root@target-board:/usr/lib/"
        echo "  ssh root@target-board 'ldconfig'"
    else
        echo "WARNING: This is x86_64 build, will NOT work on ARM target!"
        echo "Use '$0 cross' to build for ARM."
    fi
else
    echo ""
    echo "Build failed!"
    exit 1
fi
