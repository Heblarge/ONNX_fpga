#!/bin/bash

# 停止当前运行的Metals服务器（如果有）
echo "停止Metals服务器..."
pkill -f "metals"

# 清除Metals缓存
echo "清除Metals缓存..."
rm -rf .metals/
rm -rf .bloop/
rm -rf project/.bloop/
rm -rf project/metals.sbt

# 重新创建必要的Metals配置文件
echo "创建Metals配置..."
mkdir -p .metals

# 确保sbt正确获取依赖
echo "刷新SBT依赖..."
sbt update

# 完成
echo "完成！请重启VSCode，然后让Metals重新导入项目。"
echo "在VSCode中按下Ctrl+Shift+P，然后输入'Metals: Import build'来重新导入项目。"
