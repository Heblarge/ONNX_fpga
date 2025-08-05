# -*- coding: utf-8 -*-

import numpy as np
import os
import sys
import struct
import argparse

# ===================================================================
#  默认配置
# ===================================================================
GOLDEN_DIR = 'golden_outputs'
JAVA_DIR = 'java_outputs'
ORDER_FILE = 'execution_order.txt'
DEFAULT_TOLERANCE = 2e-4
# ===================================================================

def load_java_tensor(file_path):
    """从我们自定义的.bin格式中加载张量，包含读取头部（形状）信息。"""
    with open(file_path, 'rb') as f:
        rank_bytes = f.read(4)
        if len(rank_bytes) < 4: raise IOError(f"文件损坏: {file_path}，无法读取rank。")
        rank = struct.unpack('>i', rank_bytes)[0]

        shape = []
        for _ in range(rank):
            dim_bytes = f.read(8)
            if len(dim_bytes) < 8: raise IOError(f"文件损坏: {file_path}，无法完整读取shape信息。")
            dim = struct.unpack('>q', dim_bytes)[0]
            shape.append(dim)
        shape = tuple(shape)

        flat_data = np.fromfile(f, dtype='>f4')

        if np.prod(shape) != flat_data.size:
            print(f"警告：文件 {os.path.basename(file_path)} 中的形状和数据大小不匹配! 期望 Shape={shape}, 实际 Data Size={flat_data.size}")
            return None, None

        return flat_data.reshape(shape), shape

def main(args):

    target_tensor_name = '_model3_MinGRU_Layers_layers.1_Softplus_1_output_0'
    tensor_names = [target_tensor_name]
    print(f"--- 进入单个张量调试模式 ---")
    print(f"--- 目标张量: {target_tensor_name} ---")

    print(f"误差容忍度 (Tolerance): {args.tolerance}")
    print("-" * 50)

    for name in tensor_names:
        safe_name = name.lstrip('/').replace('/', '_').replace(':', '_')
        golden_path = os.path.join(GOLDEN_DIR, f"{safe_name}.npy")
        java_path = os.path.join(JAVA_DIR, f"{safe_name}.bin")

        if not os.path.exists(golden_path) or not os.path.exists(java_path):
            print(f"错误: 找不到 '{name}' 对应的文件, 请检查目录和文件名。")
            continue


        golden_tensor = np.load(golden_path)
        java_tensor, _ = load_java_tensor(java_path)

        if java_tensor is None or golden_tensor.shape != java_tensor.shape:
            print(f"失败: {name} (Java 张量加载失败或形状不匹配)")
            continue

        if np.allclose(golden_tensor, java_tensor, atol=args.tolerance):
            print(f"结果一致 (在误差 {args.tolerance} 范围内)")
        else:
            print(f"\n!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
            print(f"!!! 发现误差分岔点! 张量: {name}")
            print("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")

            diff = np.abs(golden_tensor - java_tensor)
            print(f"  - Golden (Python) shape: {golden_tensor.shape}")
            print(f"  - Java (Your) shape:   {java_tensor.shape}")
            print(f"  - 最大绝对误差: {np.max(diff):.8f}")

            try:
                # 1. 创建一个布尔矩阵，标记出所有误差超标的位置
                mismatch_mask = np.abs(golden_tensor - java_tensor) > args.tolerance
                # 2. 找到第一个为True的元素的坐标
                first_mismatch_coords = np.argwhere(mismatch_mask)[0]

                # 3. 将坐标元组转换为可用于索引的格式
                coords_tuple = tuple(first_mismatch_coords)

                # 4. 获取该位置上的两个值
                golden_val = golden_tensor[coords_tuple]
                java_val = java_tensor[coords_tuple]

                print(f"\n  第一个误差点位置 (Coordinates): {coords_tuple}")
                print(f"    - Golden 值: {golden_val}")
                print(f"    - Java 值:   {java_val}")
            except Exception as e:
                print(f"  查找具体误差位置时出错: {e}")

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description="对比 ONNX Runtime 和 Java 引擎的中间张量。")
    parser.add_argument('-t', '--tolerance', type=float, default=DEFAULT_TOLERANCE, help=f"设置数值对比的误差容忍度 (默认: {DEFAULT_TOLERANCE})")
    args = parser.parse_args()
    main(args)