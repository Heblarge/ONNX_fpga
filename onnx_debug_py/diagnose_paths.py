# -*- coding: utf-8 -*-
import os

# --- 配置 ---
GOLDEN_DIR = 'golden_outputs'
JAVA_DIR = 'java_outputs'
ORDER_FILE = 'execution_order.txt'
# ------------

print("--- 路径诊断开始 ---")

# 1. 检查所需文件和目录是否存在
print(f"\n[检查1：基础文件和目录是否存在？]")
print(f" - 脚本当前工作目录: {os.getcwd()}")
print(f" - 'execution_order.txt' 是否存在? -> {os.path.exists(ORDER_FILE)}")
print(f" - '{GOLDEN_DIR}/' 目录是否存在? -> {os.path.exists(GOLDEN_DIR)}")
print(f" - '{JAVA_DIR}/' 目录是否存在? -> {os.path.exists(JAVA_DIR)}")

# 2. 读取执行顺序文件
try:
    with open(ORDER_FILE, 'r', encoding='utf-8') as f:
        tensor_names = [line.strip() for line in f if line.strip()]
    print(f"\n[检查2：读取'execution_order.txt'文件]")
    print(f" - 成功读取 {len(tensor_names)} 个张量名。")
    if tensor_names:
        print(f" - 读取到的第一个名字是: '{tensor_names[0]}'")
except Exception as e:
    print(f" - 读取 '{ORDER_FILE}' 文件时出错: {e}")
    tensor_names = []

# 3. 列出目录中的实际文件
try:
    golden_files = os.listdir(GOLDEN_DIR)
    java_files = os.listdir(JAVA_DIR)
    print(f"\n[检查3：查看目录中的实际文件]")
    print(f" - '{GOLDEN_DIR}/' 目录中找到了 {len(golden_files)} 个文件。")
    if golden_files:
        print(f"   - 目录中第一个文件的名字是: '{golden_files[0]}'")
    print(f" - '{JAVA_DIR}/' 目录中找到了 {len(java_files)} 个文件。")
    if java_files:
        print(f"   - 目录中第一个文件的名字是: '{java_files[0]}'")
except Exception as e:
    print(f" - 列出目录文件时出错: {e}")


# 4. 对比第一个名字
if tensor_names:
    print("\n[检查4：对比第一个名字的构造结果和实际文件名]")

    # 从 execution_order.txt 读取的第一个名字
    first_name = tensor_names[0]
    print(f" 1. 从 order 文件读取的名字: '{first_name}'")

    # 脚本构造出的期望文件名
    safe_name = first_name.replace('/', '_').replace(':', '_')
    expected_golden_filename = f"{safe_name}.npy"
    print(f" 2. 脚本构造出的期望 Golden 文件名: '{expected_golden_filename}'")

    # 磁盘上实际存在的第一个文件名
    if golden_files:
        actual_first_golden_filename = golden_files[0]
        print(f" 3. 磁盘上实际存在的 Golden 文件名: '{actual_first_golden_filename}'")

        # 对比
        if expected_golden_filename == actual_first_golden_filename:
            print("  -> ✅ 构造的文件名与实际文件名【匹配】！")
        else:
            print("  -> ❌ 构造的文件名与实际文件名【不匹配】！请仔细对比第2点和第3点的字符串差异。")

print("\n--- 路径诊断结束 ---")