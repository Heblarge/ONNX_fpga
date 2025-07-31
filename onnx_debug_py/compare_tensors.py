# -*- coding: utf-8 -*-

import numpy as np
import os
import sys

# ===================================================================
#  配置区域
# ===================================================================
GOLDEN_DIR = 'golden_outputs'
JAVA_DIR = 'java_outputs'
ORDER_FILE = 'execution_order.txt'
TOLERANCE = 1e-4
# ===================================================================

def compare_tensors():
    try:
        with open(ORDER_FILE, 'r', encoding='utf-8') as f:
            tensor_names = [line.strip() for line in f if line.strip()]
    except FileNotFoundError:
        print(f"错误：找不到执行顺序文件 '{ORDER_FILE}'。")
        return

    print(f"将按照 {len(tensor_names)} 个张量的顺序进行对比...")

    found_divergence = False
    for name in tensor_names:

        # ▼▼▼ 核心修复：修正安全文件名的生成逻辑 ▼▼▼
        # 1. 使用 lstrip('/') 移除字符串开头的 '/'
        # 2. 然后再进行替换
        safe_name = name.lstrip('/').replace('/', '_').replace(':', '_')
        # ▲▲▲ 核心修复结束 ▲▲▲

        golden_path = os.path.join(GOLDEN_DIR, f"{safe_name}.npy")
        java_path = os.path.join(JAVA_DIR, f"{safe_name}.bin")

        if not os.path.exists(golden_path) or not os.path.exists(java_path):
            # 如果依然找不到文件，打印出来方便调试
            if not os.path.exists(golden_path):
                print(f"警告: 找不到 Golden 文件: {golden_path}")
            if not os.path.exists(java_path):
                print(f"警告: 找不到 Java 文件: {java_path}")
            continue

        print(f"正在对比: {name}")

        golden_tensor = np.load(golden_path)
        java_flat_tensor = np.fromfile(java_path, dtype=np.float32)

        if java_flat_tensor.size != golden_tensor.size:
            print(f"\n!!! 发现尺寸不匹配! 张量: {name}")
            found_divergence = True
            break

        java_tensor = java_flat_tensor.reshape(golden_tensor.shape)

        if not np.allclose(golden_tensor, java_tensor, atol=TOLERANCE):
            print(f"\n!!! 发现误差分岔点! 张量: {name}")
            # ... 详细打印 ...
            found_divergence = True
            break

    if not found_divergence:
        print("\n✅ 对比完成，所有已生成的张量在误差范围内完全一致！")
    else:
        print("\n对比终止。")

if __name__ == '__main__':
    compare_tensors()