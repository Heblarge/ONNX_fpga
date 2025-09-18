import numpy as np
import os
import sys
import struct
import argparse

# --- 配置区 ---
# 将两个需要比较的文件夹路径固定在这里
JAVA_DIR_1 = 'java_hw_each_layer_outputs/data1'
JAVA_DIR_2 = 'java_dl4j_each_layer_outputs/data1'
# --------------------------------------------------

# 定义执行顺序的文件
ORDER_FILE = 'execution_order.txt'
DEFAULT_TOLERANCE = 4

def load_java_tensor(file_path):
    """
    按照 Rank(int32), Shape(int64[]), Data(float32[]) 的大端序格式读取.bin文件
    """
    try:
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
                print(f"警告：文件 {os.path.basename(file_path)} 中的形状和数据大小不匹配! Shape={shape}, Data Size={flat_data.size}")
                return None, None

            return flat_data.reshape(shape), shape
    except Exception as e:
        print(f"错误: 读取文件 {file_path} 时发生异常: {e}")
        return None, None

def main(args):
    # 检查两个目录是否存在
    if not os.path.isdir(JAVA_DIR_1):
        print(f"错误：找不到HW目录 '{JAVA_DIR_1}'")
        return
    if not os.path.isdir(JAVA_DIR_2):
        print(f"错误：找不到DL4J目录 '{JAVA_DIR_2}'")
        return

    try:
        with open(ORDER_FILE, 'r', encoding='utf-8') as f:
            tensor_names = [line.strip() for line in f if line.strip()]
    except FileNotFoundError:
        print(f"错误：找不到执行顺序文件 '{ORDER_FILE}'。")
        return

    tensors_to_skip = set(args.skip)

    print(f"将按照 {len(tensor_names)} 个张量的顺序进行对比...")
    print(f"HW   目录: '{JAVA_DIR_1}'")
    print(f"DL4J 目录: '{JAVA_DIR_2}'")
    print(f"误差容忍度 (Tolerance): {args.tolerance}")
    if tensors_to_skip:
        print(f"将跳过以下 {len(tensors_to_skip)} 个张量的对比: {tensors_to_skip}")
    print("-" * 60)

    passed_count = 0
    failed_count = 0
    skipped_count = 0
    manually_skipped_count = 0

    for name in tensor_names:
        if name in tensors_to_skip:
            manually_skipped_count += 1
            continue

        safe_name = name.replace('/', '_').replace(':', '_')
        path1 = os.path.join(JAVA_DIR_1, f"{safe_name}.bin")
        path2 = os.path.join(JAVA_DIR_2, f"{safe_name}.bin")

        if not os.path.exists(path1) or not os.path.exists(path2):
            skipped_count += 1
            continue

        tensor1, shape1 = load_java_tensor(path1)
        tensor2, shape2 = load_java_tensor(path2)

        if tensor1 is None or tensor2 is None or shape1 != shape2:
            failed_count += 1
            print(f"失败: {name} (张量加载失败或形状不匹配: HW Shape={shape1}, DL4J Shape={shape2})")
            if not args.full_report: break
            continue

        if np.allclose(tensor1, tensor2, atol=args.tolerance, rtol=args.tolerance):
            passed_count += 1
            print(f"通过: {name}")
        else:
            failed_count += 1
            print(f"\n!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
            print(f"发现误差点! 张量: {name}")

            diff = np.abs(tensor1 - tensor2)
            mismatched_mask = diff > (args.tolerance + args.tolerance * np.abs(tensor2))
            mismatch_count = np.sum(mismatched_mask)
            total_elements = tensor1.size

            max_diff_index = np.unravel_index(np.argmax(diff), diff.shape)

            print(f"  - 未通过数值个数: {mismatch_count} / {total_elements}")
            print(f"  - 最大绝对误差: {np.max(diff):.8f}")
            print(f"  - 误差最大位置: {max_diff_index}")
            # **修改点**: 更新打印标签
            print(f"  - HW中的值   @ {max_diff_index}: {tensor1[max_diff_index]}")
            print(f"  - DL4J中的值 @ {max_diff_index}: {tensor2[max_diff_index]}")
            if not args.full_report:
                break

    print("\n================== 对比总结 ==================")
    print(f"总计: {len(tensor_names)} | 通过: {passed_count} | 失败: {failed_count} | 跳过(文件丢失): {skipped_count} | 跳过(手动): {manually_skipped_count}")
    print("==============================================")
    if failed_count > 0:
        print("\n对比失败！")
    else:
        print("\n恭喜！所有张量对比通过！")


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description="对比两个固定Java后端 (HW vs DL4J) 生成的中间张量 (.bin 文件)。")
    parser.add_argument('-t', '--tolerance', type=float, default=DEFAULT_TOLERANCE, help=f"设置数值对比的绝对和相对误差容忍度 (默认: {DEFAULT_TOLERANCE})")
    parser.add_argument('-f', '--full-report', action='store_true', help="开启全量报告模式，即使遇到第一个错误也会继续对比完所有张量")
    parser.add_argument(
        '-s', '--skip',
        nargs='+',
        default=[],
        help="指定一个或多个要跳过对比的张量名 (ONNX原始名，用空格分隔)"
    )

    args = parser.parse_args()
    main(args)

