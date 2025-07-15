import math
import numpy as np

def generate_atanh_lookup(scale_factor=2**12, max_iterations=12):
    atanh_lookup = []
    for j in range(1, max_iterations + 1):
        atanh_val = int(scale_factor * math.atanh(2 ** -j))
        atanh_lookup.append(atanh_val)
    return atanh_lookup


def generate_q12_values(int_range=(0, 100), frac_bits=12, skip_zero=False):
    frac_vals = np.arange(0, 2 ** frac_bits) / (2 ** frac_bits)
    int_vals = np.arange(int_range[0], int_range[1] + 1)
    q12_vals = []
    for i in int_vals:
        q12_vals.extend(i + frac_vals)
    q12_vals = np.array(q12_vals)
    if skip_zero:
        q12_vals = q12_vals[q12_vals > 0]
    return q12_vals

# CORDIC LN function with FLOAT-POINT
def cordic_ln(x, num_iterations=50):
    if x <= 0:
        raise ValueError("x must be greater than 0.")
    k = 0
    while x >= 2:
        x /= 2
        k += 1
    while x < 1:
        x *= 2
        k -= 1
    x_n, y_n, z_n = x + 1, x - 1, 0.0
    for j in range(1, num_iterations + 1):
        sign_y = math.copysign(1, y_n)
        atanh_val = math.atanh(2 ** -j)
        x_n, y_n, z_n = x_n - sign_y * (2 ** -j) * y_n, y_n - sign_y * (2 ** -j) * x_n, z_n + sign_y * atanh_val
        if j in [4, 13]:
            sign_y = math.copysign(1, y_n)
            x_n_old = x_n
            x_n = x_n - sign_y * (2 ** -j) * y_n
            y_n = y_n - sign_y * (2 ** -j) * x_n_old
            z_n = z_n + sign_y * atanh_val

    ln_x = 2 * z_n + k * math.log(2)
    return ln_x

# CORDIC LN function with FIXED-POINT simulation
def cordic_ln_fixed_point(x, scale_factor=2 ** 12, num_iterations=12, using_compensation_iters=True):
    if x <= 0:
        raise ValueError("x must be greater than 0.")
    x_scaled = int(x * scale_factor)
    k = 0
    while x_scaled >= 2 * scale_factor:
        x_scaled //= 2
        k += 1
    while x_scaled < scale_factor:
        x_scaled *= 2
        k -= 1
    x_n = x_scaled + scale_factor
    y_n = x_scaled - scale_factor
    z_n = 0
    atanh_lookup = generate_atanh_lookup(scale_factor, num_iterations)
    for j in range(1, num_iterations + 1):
        sign_y = math.copysign(1, y_n)
        atanh_val = atanh_lookup[j - 1]
        x_n_old = x_n
        x_n = x_n - sign_y * np.floor((2 ** -j) * y_n)
        y_n = y_n - sign_y * np.floor((2 ** -j) * x_n_old)
        z_n = z_n + sign_y * atanh_val
        if using_compensation_iters and j in [4, 13]:
            sign_y = math.copysign(1, y_n)
            x_n_old = x_n
            x_n = x_n - sign_y * np.floor((2 ** -j) * y_n)
            y_n = y_n - sign_y * np.floor((2 ** -j) * x_n_old)
            z_n = z_n + sign_y * atanh_val
    pre_clac_ln2 = math.log(2)
    ln_x_scaled = 2 * z_n + k * int(scale_factor * pre_clac_ln2)
    ln_x = ln_x_scaled / scale_factor
    return ln_x

# For debugging
x_values = [4097/(2 ** 12),1.0,5.85205078,1.05]
for x in x_values:
    ln_approx = cordic_ln_fixed_point(x)
    ln_true = math.log(x)

    ln_true_scaled = ln_true * (2 ** 12)
    ln_approx_scaled = ln_approx * (2 ** 12)
    error = abs((ln_approx- ln_true)/(ln_true+1e-12)) * 100
    print(f"ln({x}) ≈ {ln_approx:.6f} (true: {ln_true:.6f}, error_percent: {error:.6f})")

#Test and plot
import matplotlib
import matplotlib.pyplot as plt
matplotlib.use('TkAgg') 

x_vals = generate_q12_values(int_range=(1, 2), frac_bits=12,skip_zero=True)
approx_vals = []
approx_vals_improved = []
true_vals = []
errors = []

for x in x_vals:
    ln_approx = cordic_ln_fixed_point(x, scale_factor=2**12, num_iterations=12)
    ln_true = math.log(x)
    approx_vals.append(ln_approx)
    true_vals.append(ln_true)
    errors.append(abs(ln_approx - ln_true))

plt.figure(figsize=(12, 6))
plt.plot(x_vals, true_vals, label='math.log(x) (True)', color='blue')
plt.plot(x_vals, approx_vals, label='cordic_ln_fixed_point(x)', color='red', linestyle='--')
# plt.plot(x_vals, approx_vals_improved, label='cordic_ln_fixed_point_improved(x)', color='green', linestyle='dashdot')
plt.xlabel('x', fontsize=14)
plt.ylabel('ln(x)', fontsize=14)
plt.title('True ln(x) vs. CORDIC Fixed-Point ln(x)', fontsize=16)
plt.legend()
plt.grid(True)
plt.show()

# 绘制误差曲线
plt.figure(figsize=(12, 6))
plt.plot(x_vals, errors, label='Absolute Error', color='green')
plt.xlabel('x', fontsize=14)
plt.ylabel('Absolute Error', fontsize=14)
plt.title('Absolute Error between cordic_ln_fixed_point(x) and math.log(x)', fontsize=16)
plt.legend()
plt.grid(True)
plt.show()