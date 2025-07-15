import math
import numpy as np
import matplotlib.pyplot as plt

# 提前准备对数表L和倒数表INV_L
# iterations = 10
# L = [math.log(1 + 2 ** -k) for k in range(iterations + 1)]
# INV_L = [1 / (1 + 2 ** -k) for k in range(iterations + 1)]
# def softplus_shift_add(x, iterations=10):
#     """基于移位加法的稳定Softplus近似实现"""
#     if x < 0:
#         return softplus_shift_add(-x, iterations) - (-x)
#
#     E = 1.0
#     while x >= L[0]:
#         x -= L[0]
#         E *= 2
#
#     for k in range(1, iterations + 1):
#         if x >= L[k]:
#             x -= L[k]
#             E *= (1 + 2 ** -k)
#
#     S = 1.0 + E
#     F = 0.0
#     while S >= 2.0:
#         S /= 2.0
#         F += L[0]
#
#     for k in range(1, iterations + 1):
#         if S >= (1 + 2 ** -k):
#             S *= INV_L[k]  # 精确预计算倒数
#             F += L[k]
#
#     return F


SCALE = 1 << 12  # scale factor: 4096 (对应12位小数)
MASK = (1 << 20) - 1  # 20位总宽度掩码（模拟溢出时使用）

def float_to_fixed(x):
    """浮点数转定点数"""
    return int(round(x * SCALE))

def fixed_mul(a, b):
    """定点数乘法，结果恢复到8.12格式"""
    return (a * b) >> 12

def fixed_to_float(x):
    """定点数转浮点数"""
    return x / SCALE

iterations = 10
L_fixed = [float_to_fixed(math.log(1 + 2 ** -k)) for k in range(iterations + 1)]
INV_L_fixed = [float_to_fixed(1 / (1 + 2 ** -k)) for k in range(iterations + 1)]
LN2_fixed = float_to_fixed(math.log(2))
def softplus_fixed(x_fixed, iterations=10):
    if x_fixed >= 0:
        E_fixed = SCALE
        x_temp = x_fixed
        while x_temp >= LN2_fixed:
            x_temp -= LN2_fixed
            E_fixed <<= 1
        for k in range(1, iterations + 1):
            if x_temp >= L_fixed[k]:
                x_temp -= L_fixed[k]
                E_fixed += E_fixed >> k
        S_fixed = E_fixed + SCALE
        F_fixed = 0

        while S_fixed >= (SCALE << 1):
            S_fixed >>= 1
            F_fixed += LN2_fixed

        for k in range(1, iterations + 1):
            threshold = SCALE + (SCALE >> k)
            if S_fixed >= threshold:
                S_fixed = fixed_mul(S_fixed, INV_L_fixed[k])
                F_fixed += L_fixed[k]
        return F_fixed
    else:
        neg_x_fixed = -x_fixed
        E_fixed = SCALE
        x_temp = neg_x_fixed
        while x_temp >= LN2_fixed:
            x_temp -= LN2_fixed
            E_fixed <<= 1

        for k in range(1, iterations + 1):
            if x_temp >= L_fixed[k]:
                x_temp -= L_fixed[k]
                E_fixed += E_fixed >> k

        S_fixed = E_fixed + SCALE
        F_fixed = 0
        while S_fixed >= (SCALE << 1):
            S_fixed >>= 1
            F_fixed += LN2_fixed

        for k in range(1, iterations + 1):
            threshold = SCALE + (SCALE >> k)
            if S_fixed >= threshold:
                S_fixed = fixed_mul(S_fixed, INV_L_fixed[k])
                F_fixed += L_fixed[k]
        return x_fixed + F_fixed



# 生成数据
x_values = np.linspace(-12, 12, 500)
softplus_true = np.log1p(np.exp(x_values))
#softplus_approx_shift_add = [softplus_shift_add(x, iterations=5) for x in x_values]
softplus_fixed_results = [fixed_to_float(softplus_fixed(float_to_fixed(x))) for x in x_values]

# 绘图
plt.figure(figsize=(10, 6))
plt.plot(x_values, softplus_true, label="True Softplus", linestyle="dashed")
#plt.plot(x_values, softplus_approx_shift_add, label="Shift-Add Approximation (Float)", linestyle="solid")
plt.plot(x_values, softplus_fixed_results, label="Fixed-Point Approximation (8.12)", linestyle="solid")
plt.xlabel("x")
plt.ylabel("Softplus(x)")
plt.legend()
plt.title("Softplus Approximation: Shift-Add (Float) vs. Fixed-Point (8.12)")
plt.grid(True)
plt.show()
