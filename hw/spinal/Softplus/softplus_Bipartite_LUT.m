%% 清空环境
clear; clc; close all;

%% 定义Softplus函数及参数
f = @(t) log(1 + exp(t));   % 目标函数：softplus
t_range = [-16, 16];        % 输入范围：t ∈ [-10, 10]
t_samples=[t_range(1):0.001: t_range(2)]; 
%% 绘制原函数
figure;
plot(t_samples, f(t_samples), 'b-', 'LineWidth', 1.5);
hold on;
title('Softplus Function vs. LUT Approximation');
xlabel('t');
ylabel('Value');
grid on;

%% 设置查找表参数
K1 = 7;     % 高位索引位数
K2 = 6;     % 中位索引位数
K3 = 6;     % 低位索引位数
g1 = 10;     % P表小数部分位宽（根据输出范围调整）
g2 = 15;    % N表小数部分位宽

%% 生成查找表并分析误差
[log2maxerror, Ptable, Ntable, maxB] = Bipartite_LUT_error_analysis(...
    f,K1, K2, K3, g1, g2, t_range(1), t_range(2));
fprintf('最大误差的log2值: %.4f\n', max(log2maxerror));

%% 生成近似值并绘制对比曲线
% 组合P表和N表的值
total_points = 2^(K1+K2+K3);
alpha = zeros(total_points, 1);
for idx = 0:total_points-1
    % 分解索引为高位(k1)、中位(k2)、低位(k3)
    k1 = bitshift(idx, -(K2+K3));          % 高位
    rem = idx - bitshift(k1, K2+K3);
    k2 = bitshift(rem, -K3);              % 中位
    k3 = rem - bitshift(k2, K3);          % 低位
    alpha(idx+1) = double(Ptable(k1+1, k2+1)) + double(Ntable(k1+1, k3+1));
end

% 计算输入映射（关键修正：均匀映射到[-10,10]）
tt = t_range(1) + (0:total_points-1) * (t_range(2)-t_range(1)) / (total_points-1);
plot(tt, alpha, 'r--', 'LineWidth', 1);
legend('Original Softplus', 'LUT Approximation');

%% 计算最大绝对误差
valid_indices = (tt >= t_range(1)) & (tt <= t_range(2));
dif = abs(f(tt(valid_indices)') - alpha(valid_indices));
max_abs_error = max(dif);
fprintf('最大绝对误差: %.6f\n', max_abs_error);

%% 生成LUT文件（供硬件使用）
file_out = fopen("softplus_LUT.txt", 'w');
fprintf(file_out, '// Softplus LUT: P_table + N_table\n');
fprintf(file_out, '// K1=%d, K2=%d, K3=%d, Range=[%.1f, %.1f]\n', ...
    K1, K2, K3, t_range(1), t_range(2));

% 写入P表（格式：K1高位 + K2中位）
fprintf(file_out, '\n// P_table (%d x %d):\n', 2^K1, 2^K2);
for i1 = 1:2^K1
    for i2 = 1:2^K2
        P_val = Ptable(i1, i2);
        fprintf(file_out, '%s\n', P_val.bin);
    end
end

% 写入N表（格式：K1高位 + K3低位）
fprintf(file_out, '\n// N_table (%d x %d):\n', 2^K1, 2^K3);
for i1 = 1:2^K1
    for i3 = 1:2^K3
        N_val = Ntable(i1, i3);
        fprintf(file_out, '%s\n', N_val.bin);
    end
end
fclose(file_out);

%% 子函数：Bipartite_LUT_error_analysis

function [log2maxerror, Ptable, Ntable, maxB] = Bipartite_LUT_error_analysis(...
    func,k1, k2, k3, g1, g2, t_min, t_max)

    %% 初始化参数
    total_bits = k1 + k2 + k3;
    scale = (t_max - t_min) / (2^total_bits - 1); % 输入步长

    %% 生成P表（基于中位索引）
    [xm, xh] = meshgrid(0:2^k2-1, 0:2^k1-1);
    
    % 计算首尾差值以调整插值中心
    firstspread = alphamid(func,xh, 0, 0, k1, k2, k3, t_min, scale) ...
                - alphamid(func,xh, 0, 2^k3-1, k1, k2, k3, t_min, scale);
    lastspread = alphamid(func,xh, 2^k2-1, 0, k1, k2, k3, t_min, scale) ...
               - alphamid(func,xh, 2^k2-1, 2^k3-1, k1, k2, k3, t_min, scale);
    averagespread = (firstspread + lastspread) / 2;
    
    % 计算当前子块的差值并调整
    spread = alphamid(func,xh, xm, 0, k1, k2, k3, t_min, scale) ...
           - alphamid(func,xh, xm, 2^k3-1, k1, k2, k3, t_min, scale);
    adjust = (averagespread - spread) / 2;
    P_table = alphamid(func,xh, xm, 0, k1, k2, k3, t_min, scale) + adjust;

    %% 生成N表（基于低位索引）
    [xl, xh] = meshgrid(0:2^k3-1, 0:2^k1-1);
    firstdiff = -alphamid(func,xh, 0, 0, k1, k2, k3, t_min, scale) ...
              + alphamid(func,xh, 0, xl, k1, k2, k3, t_min, scale);
    lastdiff = -alphamid(func,xh, 2^k2-1, 0, k1, k2, k3, t_min, scale) ...
             + alphamid(func,xh, 2^k2-1, xl, k1, k2, k3, t_min, scale);
    N_table = (firstdiff + lastdiff) / 2;

    %% 转换为定点数（根据输出范围调整位宽）
    max_P = max(P_table(:));
    max_N = max(N_table(:));
    % 计算最大值的对数并确保非负
    log2_max_P = max(0, ceil(log2(max_P)));  % 确保log2(max_P)不小于0
    log2_max_N = max(0, ceil(log2(max_N)));  % 确保log2(max_N)不小于0

    % 重新计算位宽
    P_table = ufi(P_table, g1 + log2_max_P + 1, g1);  % 自动位宽
    N_table = ufi(N_table, g2 + log2_max_N + 1, g2);  % 自动位宽

    %% 误差分析
    max_error = 0;
    max_t = 0;
    total_points = 2^total_bits;
    for idx = 0:total_points-1
        % 分解索引
        k1_idx = bitshift(idx, -(k2+k3));
        rem = idx - bitshift(k1_idx, k2+k3);
        k2_idx = bitshift(rem, -k3);
        k3_idx = rem - bitshift(k2_idx, k3);
        
        % 获取近似值
        approx_val = double(P_table(k1_idx+1, k2_idx+1)) ...
                   + double(N_table(k1_idx+1, k3_idx+1));
        
        % 计算真实值
        t = t_min + idx * scale;
        true_val = log(1 + exp(t));
        
        % 更新最大误差
        error = abs(approx_val - true_val);
        if error > max_error
            max_error = error;
            max_t = t;
        end
    end
    log2maxerror = log2(max_error);
    maxB = ufi(max_t, total_bits + 1, total_bits);

    %% 返回结果
    Ptable = P_table;
    Ntable = N_table;
end

%% 子函数：计算中点函数值
function alpha_mid = alphamid(func,xh, xm, xl, k1, k2, k3, t_min, scale)
    %xh是高位段的int表示0~2^k1-1
    %xm是中位段的int表示0~2^k2-1
    %xl是低位段的int表示0~2^k3-1
    %k1, k2, k3是各个段的长度
    %t_min是输入的最小值
    %scale是将量化的输入0~2^total_bits-1映射回实际表示的输入范围的缩放因子


    % 将索引映射到实际输入值
    idx = xh*2^(+k2+k3) + xm*2^(k3) + xl;%根据输入的三个位段，拼接回实际的量化后输入整数
    t_mid = t_min + idx * scale;%将实际的量化后输入整数映射到希望表示的浮点输入
    
    alpha_mid = func(t_mid);%经过待拟合函数，得到拟合后的输出
    
end
