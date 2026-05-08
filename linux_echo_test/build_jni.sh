#!/bin/bash
#
# build_jni.sh - Build JNI library for FPGA accelerator
#
# Usage: ./build_jni.sh
#

set -e

# 配置
JAVA_HOME=${JAVA_HOME:-/usr/lib/jvm/java-11-openjdk-amd64}
CC=${CC:-gcc}
CXX=${CXX:-g++}
TARGET_LIB=libaccelerator_jni.so

echo "========================================="
echo "Building FPGA Accelerator JNI Library"
echo "========================================="
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
CXXFLAGS="-fPIC -O2 -Wall -std=c++11"
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
    echo "Output: $TARGET_LIB"
    echo ""
    echo "Install to Java library path:"
    echo "  sudo cp $TARGET_LIB /usr/local/lib/"
    echo "  sudo ldconfig"
    echo ""
    echo "Or set LD_LIBRARY_PATH:"
    echo "  export LD_LIBRARY_PATH=\$(pwd):\$LD_LIBRARY_PATH"
else
    echo ""
    echo "Build failed!"
    exit 1
fi
