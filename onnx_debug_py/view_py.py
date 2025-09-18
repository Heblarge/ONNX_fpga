# -*- coding: utf-8 -*-

import numpy as np
import sys

np.set_printoptions(threshold=sys.maxsize, linewidth=150, suppress=True)

if len(sys.argv) < 2:
    print("\n错误: 请提供一个 .npy 文件作为参数。")
    print("用法示例: python view_py.py golden_outputs2/_model1_feat_fstn_Reshape_1_output_0.npy\n")
    sys.exit(1)

# 从命令行参数获取要查看的文件路径
file_path = sys.argv[1]

try:
    # 使用 np.load() 加载 .npy 文件
    tensor = np.load(file_path)

    print(f"\n--- 正在查看文件: {file_path} ---")
    print(f"\n张量形状 (Shape): {tensor.shape}")
    print(f"数据类型 (Data Type): {tensor.dtype}")
    print("\n--- 张量内容 (Content): ---\n")
    print(tensor[0])

    print("\n-----------------------------------\n")

except FileNotFoundError:
    print(f"错误：找不到文件 '{file_path}'")
except Exception as e:
    print(f"读取文件时发生错误: {e}")