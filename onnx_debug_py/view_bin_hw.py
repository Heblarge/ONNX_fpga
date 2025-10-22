# -*- coding: utf-8 -*-

import numpy as np
import sys
import struct

np.set_printoptions(threshold=sys.maxsize, linewidth=150, suppress=True)

if len(sys.argv) < 2:
    print("\n错误: 请提供一个 .bin 文件作为参数。")
    print("用法示例: python view_bin_hw.py java_hw_each_layer_outputs_all/data1/PPQ_Variable_1201.bin\n")


    sys.exit(1)

file_path = sys.argv[1]

try:
    with open(file_path, 'rb') as f:

        rank_bytes = f.read(4)
        if not rank_bytes:
            raise IOError("文件为空或已损坏，无法读取rank。")
        rank = struct.unpack('>i', rank_bytes)[0]

        shape = []
        for _ in range(rank):
            dim_bytes = f.read(8)
            dim = struct.unpack('>q', dim_bytes)[0]
            shape.append(dim)
        shape = tuple(shape)
        f.read(4)
        flat_data = np.fromfile(f, dtype='>i4')

        if np.prod(shape) != flat_data.size:
            print(f"警告：文件中的形状和数据大小不匹配! Shape={shape}, Data Size={flat_data.size}")

        tensor = flat_data.reshape(shape)

        print(f"\n--- 正在查看文件: {file_path} ---")
        print(f"\n完整张量形状 (Full Shape): {tensor.shape}")
        print(f"数据类型 (Data Type): {tensor.dtype}")

        print("tensor: \n")
        print(tensor)
        print("\n----------------------------------------------------\n")


except FileNotFoundError:
    print(f"错误：找不到文件 '{file_path}'")
except Exception as e:
    print(f"读取文件时发生错误: {e}")