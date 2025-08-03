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
DEFAULT_TOLERANCE = 1e-4
# ===================================================================

def load_java_tensor(file_path):
    """从我们自定义的.bin格式中加载张量，包含读取头部（形状）信息。"""
    with open(file_path, 'rb') as f:
        rank_bytes = f.read(4)
        if len(rank_bytes) < 4: raise IOError("文件损坏或格式不正确，无法读取rank。")
        rank = struct.unpack('>i', rank_bytes)[0]

        shape = []
        for _ in range(rank):
            dim_bytes = f.read(8)
            if len(dim_bytes) < 8: raise IOError("文件损坏，无法完整读取shape信息。")
            dim = struct.unpack('>q', dim_bytes)[0]
            shape.append(dim)
        shape = tuple(shape)

        flat_data = np.fromfile(f, dtype='>f4')

        if np.prod(shape) != flat_data.size:
            print(f"警告：文件 {os.path.basename(file_path)} 中的形状和数据大小不匹配! 期望 Shape={shape}, 实际 Data Size={flat_data.size}")
            return None, None

        return flat_data.reshape(shape), shape

def main(args):
    """主对比函数"""
    # ▼▼▼ 恢复从 execution_order.txt 读取 ▼▼▼
    try:
        with open(ORDER_FILE, 'r', encoding='utf-8') as f:
            tensor_names = [line.strip() for line in f if line.strip()]
    except FileNotFoundError:
        print(f"错误：找不到执行顺序文件 '{ORDER_FILE}'。")
        return
    # ▲▲▲ 恢复结束 ▲▲▲

    print(f"将按照 {len(tensor_names)} 个张量的顺序进行对比...")
    print(f"误差容忍度 (Tolerance): {args.tolerance}")
    print(f"全量报告模式 (Full Report): {'开启' if args.full_report else '关闭'}")
    print("-" * 50)

    passed_count = 0
    failed_count = 0
    skipped_count = 0

    for name in tensor_names:
        safe_name = name.replace('/', '_').replace(':', '_')

        golden_path = os.path.join(GOLDEN_DIR, f"{safe_name}.npy")
        java_path = os.path.join(JAVA_DIR, f"{safe_name}.bin")

        if not os.path.exists(golden_path) or not os.path.exists(java_path):
            skipped_count += 1
            continue

        golden_tensor = np.load(golden_path)
        java_tensor, _ = load_java_tensor(java_path)

        if java_tensor is None or golden_tensor.shape != java_tensor.shape:
            failed_count += 1
            print(f"❌ 对比失败: {name} (Java 张量加载失败或形状不匹配)")
            if not args.full_report: break
            continue

        if np.allclose(golden_tensor, java_tensor, atol=args.tolerance):
            # 为了保持控制台干净，成功时可以只打印简单的信息或不打印
            print(f"✅ 对比通过: {name}")
            passed_count += 1
        else:
            failed_count += 1
            print(f"\n!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
            print(f"!!! 发现误差分岔点! 张量: {name}")
            print("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
            diff = np.abs(golden_tensor - java_tensor)
            print(f"  - 最大绝对误差: {np.max(diff):.8f}")

            # 如果需要查看完整张量，可以取消下面的注释
            # np.set_printoptions(threshold=sys.maxsize, linewidth=150, suppress=True)
            # print("\n--- EXPECTED (Golden) Tensor ---\n", golden_tensor)
            # print("\n--- ACTUAL (Java) Tensor ---\n", java_tensor)
            # np.set_printoptions(threshold=1000)

            if not args.full_report:
                break

    print("\n================== 对比总结 ==================")
    print(f"总计: {len(tensor_names)} | ✅ 通过: {passed_count} | ❌ 失败: {failed_count} | ⚠️ 跳过: {skipped_count}")
    print("==============================================")


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description="对比 ONNX Runtime 和 Java 引擎的中间张量。")
    parser.add_argument('-t', '--tolerance', type=float, default=DEFAULT_TOLERANCE, help=f"设置数值对比的误差容忍度 (默认: {DEFAULT_TOLERANCE})")
    parser.add_argument('-f', '--full-report', action='store_true', help="开启全量报告模式，即使遇到第一个错误也会继续对比完所有张量")
    args = parser.parse_args()
    main(args)