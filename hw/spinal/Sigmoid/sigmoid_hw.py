import numpy as np
import matplotlib.pyplot as plt

def sigmoid_approx_floating_point_fast(x):
    x_abs = abs(x)
    if x_abs >= 5.0:
        result_pos = 1.0  # sigmoid ≈ 1
    elif x_abs >= 2.375:
        result_pos = (x_abs * (1 / 32)) + 0.84375  # 1/32 * x + 0.84375
    elif x_abs >= 1.0:
        result_pos = (x_abs * (1 / 8)) + 0.625  # 1/8 * x + 0.625
    else:  # x_abs < 1.0
        result_pos = (x_abs * 0.25) + 0.5  # 1/4 * x + 1/2
    # 处理负数情况：sigmoid(x) = 1 - sigmoid(-x)
    result_final = 1.0 - result_pos if x < 0 else result_pos
    return result_final

# 定义 Sigmoid 近似计算，考虑硬件精度（参数化输入/输出位宽）
def sigmoid_approx_fixed_point(x_fixed, in_width=20, out_width=20, scale_bits=12):
    """
    近似计算 sigmoid(x)，仿照 SpinalHDL 代码的实现逻辑。
    采用定点数计算，输入和输出位宽均可参数化。

    :param x_fixed: 量化后的输入值（应为 in_width-bit SInt 格式的整数）
    :param in_width: 输入位宽（默认 16 位）
    :param out_width: 输出位宽（默认 12 位）
    :param scale_bits: 缩放因子位宽（默认 2^12 = 4096）
    """
    SCALE = 2**scale_bits

    assert isinstance(x_fixed, int), "输入必须是整数（定点格式）"
    assert -(1 << (in_width - 1)) <= x_fixed <= (1 << (in_width - 1)) - 1, "输入超出 in_width-bit 范围"

    x_abs = abs(x_fixed)

    # 计算各个区间的缩放斜率
    x_bound1 = int(1.0 * SCALE)  # 1.0 * SCALE
    x_bound2 = int(2.375 * SCALE)  # 2.375 * SCALE
    x_bound3 = int(5.0 * SCALE)  # 5.0 * SCALE

    # offset1 = SCALE >> 1  # 1/2 * SCALE
    # offset2 = int(0.625 * SCALE)  # 0.625 * SCALE
    # offset3 = int(0.84375 * SCALE)  # 0.84375 * SCALE

    if x_abs >= x_bound3:
        result_pos = SCALE  # sigmoid ≈ 1
    elif x_bound2 <= x_abs < x_bound3:
        result_pos = (x_abs >> 5) + int(0.84375 * SCALE)
    elif x_bound1 <= x_abs < x_bound2:
        result_pos = (x_abs >> 3) + int(0.625 * SCALE)
    else:  # x_abs < x_bound1
        result_pos = (x_abs >> 2) + (SCALE >> 1)

    # 计算最终结果（1 - sigmoid(|x|) 处理负数）
    if x_fixed < 0:
        result_final = SCALE - result_pos  # 1 - sigmoid(|x|)
    else:
        result_final = result_pos

    # 限制输出范围到 out_width-bit（无符号数）
    MAX_OUT = (1 << out_width) - 1
    result_final = min(result_final, MAX_OUT)

    return result_final  # 返回定点数结果

# 量化输入函数（参数化位宽）
def quantize_input(x, in_width=16, scale_bits=12):
    SCALE = 2**scale_bits  # 计算缩放因子
    MAX_INT = (1 << (in_width - 1)) - 1
    MIN_INT = -(1 << (in_width - 1))
    x_fixed = int(x * SCALE)  # 量化
    x_fixed = max(min(x_fixed, MAX_INT), MIN_INT)
    return x_fixed


in_width = 16
out_width = 12
scale_bits = 12
import matplotlib
matplotlib.use('TkAgg')
x_vals = np.linspace(-10, 10, 1000)  # 取 -6 到 6 之间的值
x_quantized = [quantize_input(x, in_width, scale_bits) for x in x_vals]  # 确保每个元素都是 Python int 类型

# 计算定点 Sigmoid 近似值，并转换回浮点格式用于绘图
y_approx_fixed = np.array([sigmoid_approx_fixed_point(x_q, in_width, out_width, scale_bits) for x_q in x_quantized], dtype=np.float32) / (2**scale_bits)
# 真实 Sigmoid 计算
y_true = 1 / (1 + np.exp(-x_vals))
y_approx_float = np.array([sigmoid_approx_floating_point_fast(x) for x in x_vals])

# 画出对比图
plt.figure(figsize=(8, 5))
plt.plot(x_vals, y_approx_fixed, label=f"Approx Sigmoid (Fixed Point {in_width}-bit → {out_width}-bit)", linestyle="--")
plt.plot(x_vals, y_approx_float, label="Approx Sigmoid (Optimized Floating Point)", linestyle=":")
plt.plot(x_vals, y_true, label="True Sigmoid", alpha=0.7)
plt.xlabel("x")
plt.ylabel("Sigmoid(x)")
plt.title(f"Sigmoid Approximation ({in_width}-bit → {out_width}-bit)")
plt.legend()
plt.grid()
plt.show()
