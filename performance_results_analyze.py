import pandas as pd
import matplotlib.pyplot as plt
import os
from typing import List, Dict, Tuple

"""
- 读取 CSV
- 去除无关列：TestID, RunID, TotalCycles, CyclesPerTest, Status, ErrorMessage
- 选择每一个参数作为 X 轴（axis_param）
- 其余所有参数取相同组合为一条折线，绘制 FLOPSPerCycle 随该轴变化的折线图
- 对重复 (axis_param, 其它参数组合) 的多行，使用均值聚合
- 为避免图像过于拥挤，默认只展示覆盖点数最多且均值最高的前 N 条折线（可配置）
- 生成每个轴一个 PNG 文件

用法：
    python flops_visualizer.py --csv your_data.csv --out figures --max-lines 12
"""

import argparse

TARGET = "FLOPSPerCycle"
EXCLUDE_COLS = [
    "TestID",
    "RunID",
    "TotalCycles",
    "CyclesPerTest",
    "Status",
    "ErrorMessage",
]


def load_data(csv_path: str) -> pd.DataFrame:
    df = pd.read_csv(csv_path)
    # 丢弃不需要的列（若存在）
    drop_cols = [c for c in EXCLUDE_COLS if c in df.columns]
    if drop_cols:
        df = df.drop(columns=drop_cols)

    # 尝试把数值列转换为数值类型
    for c in df.columns:
        if c == TARGET:
            continue
        df[c] = pd.to_numeric(df[c], errors="ignore")
    return df


def get_param_columns(df: pd.DataFrame) -> List[str]:
    return [c for c in df.columns if c != TARGET]


def format_group_label(group: Dict[str, object], max_len: int = 80) -> str:
    parts = [f"{k}={group[k]}" for k in group]
    label = ", ".join(parts)
    if len(label) > max_len:
        label = label[: max_len - 3] + "..."
    return label


def plot_axis_lines(
    df: pd.DataFrame,
    axis_param: str,
    out_dir: str,
    max_lines: int = 12,
):
    os.makedirs(out_dir, exist_ok=True)

    param_cols = get_param_columns(df)
    other_cols = [c for c in param_cols if c != axis_param]

    lines: List[Tuple[Dict[str, object], pd.Series]] = []

    # 将相同其它参数组合的样本合成一条折线：x 为 axis_param，y 为 TARGET（按 x 聚合均值）
    # 为了高效，先按其它参数分组
    grouped = df.groupby(other_cols, dropna=False)
    for group_vals, gdf in grouped:
        # group_vals 可能是标量或元组，统一转 dict
        if len(other_cols) == 1:
            group_dict = {other_cols[0]: group_vals}
        else:
            group_dict = dict(zip(other_cols, group_vals))

        # 对该组内，按轴取均值，并按 x 排序
        series = gdf.groupby(axis_param)[TARGET].mean().sort_index()
        # 只有当该组在该轴上有>=2个不同取值时，才有折线意义
        if series.index.nunique() >= 2:
            lines.append((group_dict, series))

    if not lines:
        print(f"[WARN] 轴 {axis_param} 没有可画的折线（可能该轴在数据中只取一个值）")
        return

    # 排序选择：优先选择覆盖点数多的折线；同等再按均值高低
    lines.sort(key=lambda x: (x[1].shape[0], x[1].mean()), reverse=True)
    selected = lines[:max_lines]

    # 画图
    plt.figure(figsize=(10, 6))
    for group_dict, series in selected:
        x = series.index.values
        y = series.values
        label = format_group_label(group_dict)
        plt.plot(x, y, marker="o", linewidth=1.5, label=label)

    # 全局均值线（辅助参考）：所有样本在该轴上的平均
    overall = df.groupby(axis_param)[TARGET].mean().sort_index()
    if overall.shape[0] >= 2:
        plt.plot(overall.index.values, overall.values, linestyle="--", linewidth=2, label="Overall mean")

    plt.title(f"{TARGET} vs {axis_param} (lines: other params fixed)")
    plt.xlabel(axis_param)
    plt.ylabel(TARGET)
    # 图例放到外侧，避免遮挡
    plt.legend(loc="upper left", bbox_to_anchor=(1.02, 1.0), borderaxespad=0, fontsize=8)
    plt.tight_layout()

    out_path = os.path.join(out_dir, f"{axis_param}_lines.png")
    plt.savefig(out_path, dpi=150)
    plt.close()
    print(f"✅ Saved: {out_path}")



def visualize_all_axes(
    df: pd.DataFrame,
    out_dir: str = "performance_results_analyze",
    max_lines: int = 12,
):
    os.makedirs(out_dir, exist_ok=True)
    params = get_param_columns(df)

    for axis_param in params:
        if axis_param == TARGET:
            continue
        plot_axis_lines(df, axis_param, out_dir, max_lines=max_lines)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="FLOPSPerCycle 多轴折线可视化")
    parser.add_argument("--csv",default='performance_results.csv', help="CSV 文件路径")
    parser.add_argument("--out", default="performance_results_analyze", help="输出目录")
    parser.add_argument("--max-lines", type=int, default=12, help="每张图显示的最大折线数")
    args = parser.parse_args()

    df = load_data(args.csv)
    visualize_all_axes(df, out_dir=args.out, max_lines=args.max_lines)
    print("🎉 全部完成！")


