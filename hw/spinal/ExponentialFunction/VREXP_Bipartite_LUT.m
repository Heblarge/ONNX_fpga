clear; clf;  % 清空工作区变量并关闭所有图形窗口

% 定义时间范围和函数
t = [-1:0.001:16];  % 定义时间范围 t 从 -1 到 16，步长为 0.001
f = @(t)(1 - exp(-exp(t) .* sqrt(3)/1024));  % 定义目标函数 f(t) = 1 - exp(-exp(t) * sqrt(3)/1024)

% 绘制函数图像
plot(t, f(t));  % 绘制函数 f(t) 的图像
hold on;  % 保持当前图形窗口，以便后续绘图叠加

% 计算函数在特定点的值
f(9)  % 计算 t=9 时的函数值
f(-1)  % 计算 t=-1 时的函数值

% 设置查找表参数
K1 = 6;  % 高位索引位数
K2 = 4;  % 中位索引位数
K3 = 4;  % 低位索引位数

% 调用误差分析函数生成查找表
[log2maxerror, Ptable, Ntable, maxB] = Bipartite_LUT_error_analysis(6, 4, 4, -1, 1);  % 生成 P 表和 N 表，并计算最大误差
max(log2maxerror)  % 输出最大误差的 log2 值

% 初始化近似值数组
alpha = zeros(2^(K1+K2+K3), 1);  % 初始化 alpha 数组，用于存储近似值

% 遍历所有可能的索引组合，计算近似值
for k1 = 0:2^K1-1
    for k2 = 0:2^K2-1
        for k3 = 0:2^K3-1
            % 计算当前索引对应的近似值
            alpha(k1*2^(K2+K3) + k2*2^K3 + k3 + 1) = double(Ptable(k1+1, k2+1)) + double(Ntable(k1+1, k3+1));
        end
    end    
end

% 计算输入映射
tt = ((0:2^(K1+K2+K3)-1)) / 2^(K1+K2+K3-4) - 1;  % 将索引映射到实际输入值范围

% 绘制近似值曲线
plot(tt, alpha);  % 绘制近似值曲线

% 打开文件以写入查找表数据
file_out = fopen("doubleExpLUT.txt", 'w');

% 写入文件头信息
fprintf(file_out, ['//approx func: 1 - exp(-exp(t-1) * sqrt(3)/1024) as: P_table(t) + N_table(t),\n' ...
    ', where t is UFix(4 exp, -(k1+k2+k3-4) exp), and k1=%s, k2=%s, k3=%s.\n'], num2str(K1), num2str(K2), num2str(K3));

% 写入 P 表数据
for i1 = 1:2^(K1)
    for i2 = 1:2^(K2)
        P_mantissa = Ptable(i1, i2);  % 获取 P 表当前条目的值
        fprintf(file_out, '%s\n', P_mantissa.bin);  % 将二进制值写入文件
    end
end

% 写入 N 表数据
for i1 = 1:2^(K1)
    for i3 = 1:2^(K3)
        N_mantissa = Ntable(i1, i3);  % 获取 N 表当前条目的值
        fprintf(file_out, '%s\n', N_mantissa.bin);  % 将二进制值写入文件
    end
end

% 关闭文件
fclose(file_out);

% 示例：获取 P 表和 N 表的某个条目并输出其二进制值
a = Ptable(4, 16);  % 获取 P 表中第 4 行第 16 列的值
a.bin  % 输出该值的二进制表示
a = Ntable(4, 16);  % 获取 N 表中第 4 行第 16 列的值
a.bin  % 输出该值的二进制表示

%% 子函数：Bipartite_LUT_error_analysis
function [log2maxerror, Ptable, Ntable, maxB] = Bipartite_LUT_error_analysis(k1, k2, k3, g1, g2)
    %% 准备向量化 xh, xm, xl
    xh_range = (0:1:2^k1-1);  % 高位索引范围
    xm_range = (0:1:2^k2-1);  % 中位索引范围
    xl_range = (0:1:2^k3-1);  % 低位索引范围

    %% P 表生成
    [xm, xh] = meshgrid(xm_range, xh_range);  % 生成网格点

    % 计算首尾差值以调整插值中心
    firstspread = alphamid(xh, 0, 0, k1, k2, k3) - alphamid(xh, 0, 2^(k3)-1, k1, k2, k3);
    lastspread = alphamid(xh, 2^(k2)-1, 0, k1, k2, k3) - alphamid(xh, 2^(k2)-1, 2^(k3)-1, k1, k2, k3);
    averagespread = (firstspread + lastspread) / 2;  % 计算平均差值

    % 计算当前子块的差值并调整
    spread = alphamid(xh, xm, 0, k1, k2, k3) - alphamid(xh, xm, 2^(k3)-1, k1, k2, k3);
    adjust = (averagespread - spread) / 2;  % 计算调整量
    P_table = alphamid(xh, xm, 0, k1, k2, k3) + adjust;  % 生成 P 表

    %% N 表生成
    [xl, xh] = meshgrid(xl_range, xh_range);  % 生成网格点

    % 计算首尾差值以生成 N 表
    firstdiff = -alphamid(xh, 0, 0, k1, k2, k3) + alphamid(xh, 0, xl, k1, k2, k3);
    lastdiff = -alphamid(xh, 2^(k2)-1, 0, k1, k2, k3) + alphamid(xh, 2^(k2)-1, xl, k1, k2, k3);
    N_table = (firstdiff + lastdiff) / 2;  % 生成 N 表

    %% 格式转换
    if exist('g1', 'var') && exist('g2', 'var')
        % 将 P 表和 N 表转换为定点数格式
        P_table = ufi(P_table, (k1+k2+k3+g1+1), (k1+k2+k3+g1));
        N_table = ufi(N_table, (k1+k2+k3+g2+1), (k1+k2+k3+g2));
    end

    %% 误差分析
    max_error = 0;  % 初始化最大误差
    max_t = 0;  % 初始化最大误差对应的 t 值

    for i = 1:2^k1
        %% 计算查表结果
        P_xhxm = double(P_table(i, :));  % 获取 P 表当前行的值
        N_xhxl = double(N_table(i, :));  % 获取 N 表当前行的值
        N_xhxl = N_xhxl.';  % 转置 N 表的值
        alpha = P_xhxm + N_xhxl;  % 计算近似值
        alpha = alpha(:);  % 将结果展平为列向量

        % 计算全表最大误差与对应 t 的值
        for k = 0:1
            t = ((i-1)*2^(k2+k3) : (i)*2^(k2+k3)-1) + k;
            t = double(t) / 2^(k1+k2+k3-4);  % 将索引映射到实际 t 值
            alpha_ref = 1 - exp(-exp(t-1) .* sqrt(3)/1024);  % 计算参考值
            error = abs(alpha - alpha_ref.');  % 计算误差
            [max_error_tmp, max_t_tmp] = max(error);  % 寻找当前分块中最大误差与对应 t 值
            if max_error_tmp > max_error  % 更新全局最大误差与对应 t 值
                max_error = max_error_tmp;
                max_t = max_t_tmp + (i-1)*2^(k2+k3);
            end
        end
    end

    % 返回最大误差与对应 t 值
    log2maxerror = log2(max_error);  % 最大误差的 log2 值
    maxB = ufi((1+(max_t-1)*2^(-k1-k2-k3)), (k1+k2+k3+1), (k1+k2+k3));  % 最大误差对应的 t 值

    % 返回 P 表和 N 表
    Ptable = P_table;
    Ntable = N_table;
end

%% 子函数：计算中点原函数值
function alpha_mid = alphamid(xh, xm, xl, k1, k2, k3)
    % 计算当前子块的中点 t 值
    t_mid = xh*2^(4-k1) + xm*2^(4-k1-k2) + xl*2^(4-k1-k2-k3) + 1*2^(4-k1-k2-k3-1);
    % 计算中点对应的函数值
    alpha_mid = 1 - exp(-exp(t_mid-1) .* sqrt(3)/1024);
end