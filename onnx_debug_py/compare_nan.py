# -*- coding: utf-8 -*-

import numpy as np
import os
import sys
import struct
import argparse

# --- 配置 ---
GOLDEN_DIR = 'golden_outputs'
JAVA_DIR = 'java_outputs'
TARGET_TENSOR_NAME = 'PPQ_Variable_1512' # 锁定我们关心的目标
# ------------

def load_java_tensor(file_path):
    """从.bin格式中加载张量"""
    with open(file_path, 'rb') as f:
        rank_bytes = f.read(4)
        if len(rank_bytes) < 4: raise IOError("文件损坏，无法读取rank。")
        rank = struct.unpack('>i', rank_bytes)[0]
        shape = tuple(struct.unpack(f'>{rank}q', f.read(8 * rank)))
        flat_data = np.fromfile(f, dtype='>f4')
        if np.prod(shape) != flat_data.size:
            print(f"警告：文件 {os.path.basename(file_path)} 形状与数据大小不匹配!")
            return None
        return flat_data.reshape(shape)

def main():
    print(f"--- ⚠️  进入 NaN 查找模式 ---")
    print(f"--- 目标张量: {TARGET_TENSOR_NAME} ---")
    print("-" * 50)

    safe_name = TARGET_TENSOR_NAME.lstrip('/').replace('/', '_').replace(':', '_')
    java_path = os.path.join(JAVA_DIR, f"{safe_name}.bin")

    if not os.path.exists(java_path):
        print(f"❌ 错误: 找不到 Java 输出文件: {java_path}")
        return

    java_tensor = load_java_tensor(java_path)
    if java_tensor is None: return

    # ▼▼▼ 核心逻辑：检查是否存在 NaN ▼▼▼
    if not np.isnan(java_tensor).any():
        print("✅ 在 Java 输出中没有找到 NaN 值。问题可能在于其他类型的数值误差。")
        # 如果没有NaN，我们可以进行标准的对比
        golden_path = os.path.join(GOLDEN_DIR, f"{safe_name}.npy")
        if os.path.exists(golden_path):
            golden_tensor = np.load(golden_path)
            diff = np.abs(golden_tensor - java_tensor)
            print(f"  - 最大绝对误差: {np.max(diff):.8f}")
    else:
        print(f"\n!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
        print(f"!!! 发现 NaN 值! 张量: {TARGET_TENSOR_NAME}")
        print("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")

        # 找出第一个 NaN 的坐标
        nan_mask = np.isnan(java_tensor)
        first_nan_coords = np.argwhere(nan_mask)[0]
        coords_tuple = tuple(first_nan_coords)

        print(f"\n  第一个 NaN 出现的位置 (Coordinates): {coords_tuple}")

        # 尝试获取该位置的标准值进行对比
        golden_path = os.path.join(GOLDEN_DIR, f"{safe_name}.npy")
        if os.path.exists(golden_path):
            golden_tensor = np.load(golden_path)
            if golden_tensor.shape == java_tensor.shape:
                golden_val = golden_tensor[coords_tuple]
                print(f"    - Golden 值 (在该位置): {golden_val}")
                print(f"    - Java 值   (在该位置): NaN")

if __name__ == '__main__':
    main()