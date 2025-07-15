# 环境重置，需重新导入库
import numpy as np
import matplotlib.pyplot as plt


x_vals = np.linspace(-10, 10, 1000)
sigmoid_exact = 1 / (1 + np.exp(-x_vals))
scale = 12
# 定点参数与分段定义（确保连续性）
def quantize_scale12(val):
    """严格12位定点数量化（显示完整15位小数）"""
    return round(val * 4096) / 4096  # 结果将自动保留精确的1/4096步长

# 修正后的正区间参数（所有数值均为4096分母分数）
pos_segments = [
    # x_start (frac), x_end (frac), slope (frac), intercept (frac)
    (0 * 4096,  2046,  1047,  2048),  # x∈[0, 0.49951171875)
    (2046,    4992,   816,   2160),  # x∈[0.49951171875, 1.21826171875)
    (4992,    7968,   548,   2450),
    (7968,    10944,  340,   2862),
    (10944,   13920,  192,   3293),
    (13920,   16896,  104,   3597),
    (16896,   19872,   52,   3812),
    (19872,   22848,   26,   3920),
    (22848,   25824,   12,   4005),
    (25824,   28800,    6,   4048),
    (28800,   31776,    3,   4072),
    (31776,   32768,    1,   4086)   # x∈[7.6875, 8.0)
]

# 转换为浮点显示（保留完整小数位）
pos_segments_float = [
    (start/4096, end/4096, slope/4096, intercept/4096)
    for start, end, slope, intercept in pos_segments
]

# 验证第一个区间的正确性
first_seg = pos_segments_float[0]
print(f"x_start: {first_seg[0]:.15f}")    # 0.000000000000000
print(f"x_end:   {first_seg[1]:.15f}")    # 0.499511718750000
print(f"slope:   {first_seg[2]:.15f}")    # 0.255615234375000 (1047/4096)
print(f"intercept:{first_seg[3]:.15f}\n")  # 0.500000000000000 (2048/4096)
# 严格连续的定点分段逼近函数
def sigmoid_approx_fixed_point_continuous(x):
    if x <= -8.0:
        return 0.0
    if x >= 8.0:
        return 1.0

    for (x1, x2, m, b) in pos_segments_float:
        if x >= x1 and x < x2:
            return m * x + b
    # 对称性
    if x < 0:
        return 1.0 - sigmoid_approx_fixed_point_continuous(-x)
    return 1.0

# 绘制对比曲线与误差
x_vals = np.linspace(-10, 10, 1000)
sigmoid_exact = 1 / (1 + np.exp(-x_vals))
sigmoid_continuous = np.array([sigmoid_approx_fixed_point_continuous(x) for x in x_vals])
error_continuous = np.abs(sigmoid_exact - sigmoid_continuous)

# 绘制Sigmoid逼近曲线
plt.figure(figsize=(10, 6))
plt.plot(x_vals, sigmoid_exact, label='Exact Sigmoid', linewidth=2)
plt.plot(x_vals, sigmoid_continuous, label='Continuous Fixed-Point Approximation', linestyle='--')
plt.grid(True)
plt.legend()
plt.title('Continuous Fixed-Point Sigmoid Approximation')
plt.xlabel('x')
plt.ylabel('Sigmoid(x)')
plt.show()

# 绘制误差曲线
plt.figure(figsize=(10, 6))
plt.plot(x_vals, np.abs(sigmoid_exact - sigmoid_continuous), label='Approximation Error', linestyle='-.')
plt.grid(True)
plt.legend()
plt.title('Approximation Error of Continuous Fixed-Point Sigmoid')
plt.xlabel('x')
plt.ylabel('Absolute Error')
plt.show()

# 输出最大误差
max_error_continuous = np.max(error_continuous)
max_error_continuous
