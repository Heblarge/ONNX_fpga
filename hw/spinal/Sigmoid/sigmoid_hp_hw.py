#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
根据给定的分段参数，首先将斜率（slope）与截距（intercept）按 scale=12 定点数精度进行量化，
即每个参数均满足 $$v=\frac{k}{4096}$$，其中 $$k\in\mathbb{Z}$$。
随后，利用量化后的参数构造分段线性函数：
    $$ f(x)=m_i x+b_i,\quad x\in[x_i,x_{i+1}], $$
该近似函数用于模拟 Sigmoid 函数。此处引入分段线性逼近的核心价值在于简化硬件实现，
使得电路中仅需加法和乘法操作，从而将微分运算转换为代数运算。
"""

import numpy as np
import matplotlib.pyplot as plt
plt.rcParams['font.sans-serif'] = ['SimHei']
plt.rcParams['axes.unicode_minus'] = False
# 定义量化函数，将浮点数 v 量化为 scale=12 的定点数（每个最小步长为 1/4096）
def quantize(v, scale=12):
    factor = 2**scale  # 2^12 = 4096
    return round(v * factor) / factor

# 原始分段参数数据（区间、斜率 m、截距 b）
# 注意：各参数单位均为浮点数，后续将对 m 和 b 进行量化处理
segments = [
    (-8.000000000000, -7.058837890625, 0.000488281250, 0.004150390625),
    (-7.058837890625, -6.117675781250, 0.001464843750, 0.011072516441),
    (-6.117675781250, -5.176513671875, 0.003173828125, 0.021613717079),
    (-5.176513671875, -4.235351562500, 0.008789062500, 0.050623655319),
    (-4.235351562500, -3.294189453125, 0.021240234375, 0.103387594223),
    (-3.294189453125, -2.353027343750, 0.052001953125, 0.204751551151),
    (-2.353027343750, -1.411865234375, 0.113037109375, 0.348254799843),
    (-1.411865234375, -0.470703125000, 0.203613281250, 0.476195216179),
    (-0.470703125000, 0.470703125000, 0.254150390625, 0.500000476837),
    (0.470703125000, 1.411865234375, 0.203613281250, 0.523787498474),
    (1.411865234375, 2.353027343750, 0.113037109375, 0.651686131954),
    (2.353027343750, 3.294189453125, 0.052001953125, 0.795362591743),
    (3.294189453125, 4.235351562500, 0.021484375000, 0.895779132843),
    (4.235351562500, 5.176513671875, 0.008300781250, 0.951659679413),
    (5.176513671875, 6.117675781250, 0.003662109375, 0.975671947002),
    (6.117675781250, 7.058837890625, 0.001220703125, 0.990579009056),
    (7.058837890625, 8.000000000000, 0.000488281250, 0.995820879936)
]

# 对每个分段参数进行量化，确保所有参数均为 scale=12 定点数可表达的数值
quantized_segments = []
for (x_start, x_end, m, b) in segments:
    qm = quantize(m, scale=12)
    qb = quantize(b, scale=12)
    quantized_segments.append((x_start, x_end, qm, qb))
    # 输出调试信息：原始值与量化后的值（此处仅供验证）
    # print(f"x in [{x_start}, {x_end}]: 原 m = {m}, 量化后 m = {qm}; 原 b = {b}, 量化后 b = {qb}")

# 定义分段线性近似 Sigmoid 函数
def piecewise_sigmoid(x):
    """
    对于给定输入 x，根据所属区间选择对应的线性函数进行计算
    """
    for (x_start, x_end, m, b) in quantized_segments:
        if x_start <= x <= x_end:
            return m * x + b
    # 若 x 超出设计区间，则采用端点外推
    if x < quantized_segments[0][0]:
        m, b = quantized_segments[0][2], quantized_segments[0][3]
        return m * x + b
    else:
        m, b = quantized_segments[-1][2], quantized_segments[-1][3]
        return m * x + b

# 生成 x 值，用于绘图（从 -8 到 8 之间均匀采样）
x_vals = np.linspace(-8, 8, 800)
# 计算对应的 f(x) 值（此处引入分段函数的连续性保障）
y_vals = np.array([piecewise_sigmoid(x) for x in x_vals])

# 绘制分段线性近似的 Sigmoid 函数
plt.figure(figsize=(8, 4))
plt.plot(x_vals, y_vals, label='线性近似的 Sigmoid')
plt.xlabel('x')
plt.ylabel('f(x)')
plt.title('基于 scale=12 定点数参数的分段线性 Sigmoid 近似')
plt.legend()
plt.grid(True)
plt.show()
