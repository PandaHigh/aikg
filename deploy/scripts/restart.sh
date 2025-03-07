#!/bin/bash

# AIKG 应用重启脚本

# 设置脚本所在目录为工作目录
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# 停止应用
echo "正在停止AIKG应用..."
"$SCRIPT_DIR/stop.sh"

# 等待几秒确保应用完全停止
sleep 5

# 启动应用
echo "正在启动AIKG应用..."
"$SCRIPT_DIR/start.sh" 