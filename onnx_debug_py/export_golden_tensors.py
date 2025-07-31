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
    """
    按照 execution_order.txt 的顺序，对比并打印所有 golden 和 java 的输出张量。
    """
    try:
        with open(ORDER_FILE, 'r', encoding='utf-8') as f:
            tensor_names = [line.strip() for line in f if line.strip()]
    except FileNotFoundError:
        print(f"错误：找不到执行顺序文件 '{ORDER_FILE}'。")
        return

    print(f"将按照 {len(tensor_names)} 个张量的顺序进行对比...\n")


    found_divergence = False

    # ▼▼▼ 核心修改部分：现在循环会走完所有张量 ▼▼▼
    for name in tensor_names:
        safe_name = name.replace('/', '_').replace(':', '_')

        golden_path = os.path.join(GOLDEN_DIR, f"{safe_name}.npy")
        java_path = os.path.join(JAVA_DIR, f"{safe_name}.bin")

        if not os.path.exists(golden_path) or not os.path.exists(java_path):
            continue

        print(f"================== 正在对比张量: {name} ==================")

        golden_tensor = np.load(golden_path)
        java_flat_tensor = np.fromfile(java_path, dtype=np.float32)

        if java_flat_tensor.size != golden_tensor.size:
            print(f"!!! 尺寸不匹配! Golden: {golden_tensor.size}, Java: {java_flat_tensor.size}")
            found_divergence = True
            continue # 继续对比下一个

        java_tensor = java_flat_tensor.reshape(golden_tensor.shape)

        # 检查是否一致
        is_close = np.allclose(golden_tensor, java_tensor, atol=TOLERANCE)

        # 打印摘要信息
        diff = np.abs(golden_tensor - java_tensor)
        print(f"  - 形状 (Golden vs Java): {golden_tensor.shape} vs {java_tensor.shape}")
        print(f"  - 最大绝对误差: {np.max(diff):.8f}")
        print(f"  - 结果是否一致 (误差<{TOLERANCE}): {'✅ 是' if is_close else '❌ 否'}")

        # 设置打印选项并打印完整张量
        np.set_printoptions(threshold=sys.maxsize, linewidth=150, suppress=True)

        print("\n--- EXPECTED (Golden) Tensor ---")
        print(golden_tensor)

        print("\n--- ACTUAL (Java) Tensor ---")
        print(java_tensor)

        print("-" * 60 + "\n")

        if not is_close:
            found_divergence = True
            # 注意：这里的 break 被移除了，所以会继续对比下一个张量

    # ▲▲▲ 核心修改结束 ▲▲▲

    print("\n================== 对比总结 ==================")
    if not found_divergence:
        print("✅ 全部对比完成，所有已生成的张量在误差范围内完全一致！")
    else:
        print("❌ 对比完成，在上面的日志中发现至少一处误差！")

if __name__ == '__main__':
    compare_tensors()