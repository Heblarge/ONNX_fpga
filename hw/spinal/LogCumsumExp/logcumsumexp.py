import torch

def nlogn_cumsum(input_tensor, dim=0):
    output = input_tensor.clone()  # 复制输入张量
    n = input_tensor.shape[dim]    # 获取指定维度的长度
    s = 1                          # 初始化步长

    while s < n:
        # 生成移位后的张量：将 output 沿 dim 方向向右移动 s 位，左侧补零
        sliced = output.narrow(dim, 0, n - s)  # 截取前 n-s 个元素

        # 创建零张量，形状与 output 一致，但 dim 维度大小为 s
        zero_shape = list(output.shape)
        zero_shape[dim] = s
        zeros = torch.zeros(zero_shape, dtype=output.dtype, device=output.device)

        # 拼接零张量和 sliced
        shifted = torch.cat([zeros, sliced], dim=dim)

        # 累加到当前结果
        output = output + shifted
        s *= 2  # 步长翻倍

    return output
#zhoujs's stable logcumsumexp:
def nlogn_logcumsumexp(diff):
    # 复制输入张量
    output = diff.clone()
    n = diff.size(1)  # 获取第二个维度的长度（列数）

    # 生成所有 s 的值为一个列表
    s_values = []
    s = 1
    while s < n:
        s_values.append(s)
        s *= 2

    # 迭代 s_values 列表
    for s in s_values:
        # 直接使用原始张量的切片，避免补 -inf
        sliced = output.narrow(1, 0, n - s)  # 截取前 n-s 个元素

        # 计算 log-sum-exp
        max_val = torch.maximum(output[:, s:], sliced)  # 计算最大值平移
        exp1 = torch.exp(output[:, s:] - max_val)
        exp2 = torch.exp(sliced - max_val)

        # 使用 torch.cat 实现等价的赋值操作
        updated_part = max_val + torch.log(exp1 + exp2)
        output = torch.cat([output[:, :s], updated_part], dim=1)

    return output

if __name__ == "__main__":
    # 测试例子

    # 1. 一维张量测试
    x1 = torch.tensor([1, 2, 3, 4, 5]).float()
    print("一维张量:")
    print("原始:", x1)
    print("custom_cumsum:", nlogn_cumsum(x1, dim=0))
    print("torch.cumsum:", torch.cumsum(x1, dim=0))
    print()

    # 2. 二维张量，沿 dim=0（按行）累加
    x2 = torch.tensor([[1, 2, 3],
                       [4, 5, 6],
                       [7, 8, 9]]).float()
    print("二维张量沿 dim=0:")
    print("原始:\n", x2)
    print("custom_cumsum (dim=0):\n", nlogn_cumsum(x2, dim=0))
    print("torch.cumsum (dim=0):\n", torch.cumsum(x2, dim=0))
    print()

    # 3. 二维张量，沿 dim=1（按列）累加
    print("二维张量沿 dim=1:")
    print("原始:\n", x2)
    print("custom_cumsum (dim=1):\n", nlogn_cumsum(x2, dim=1))
    print("torch.cumsum (dim=1):\n", torch.cumsum(x2, dim=1))


    x3= torch.randn(2, 8, 4)
    y1 = (x3.clone()).logcumsumexp(dim = 1)

    # 修改后的实现
    y2 = nlogn_logcumsumexp(x3.clone())

    print(torch.allclose(y1, y2, atol=1e-4))  # 结果应输出True
