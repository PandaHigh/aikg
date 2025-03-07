#!/bin/bash

# AIKG 应用停止脚本

# 设置脚本所在目录为工作目录
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
APP_DIR="$( cd "$SCRIPT_DIR/../.." && pwd )"
PID_FILE="$APP_DIR/aikg.pid"

# 检查PID文件是否存在
if [ ! -f "$PID_FILE" ]; then
    echo "PID文件不存在，应用可能未运行"
    exit 0
fi

# 读取PID
PID=$(cat "$PID_FILE")

# 检查进程是否存在
if ! ps -p "$PID" > /dev/null; then
    echo "进程不存在，PID: $PID"
    echo "删除过期的PID文件"
    rm "$PID_FILE"
    exit 0
fi

# 尝试优雅停止应用
echo "正在停止AIKG应用，PID: $PID..."
kill "$PID"

# 等待应用停止
echo "等待应用停止..."
TIMEOUT=30
COUNT=0
while ps -p "$PID" > /dev/null; do
    sleep 1
    COUNT=$((COUNT + 1))
    if [ $COUNT -ge $TIMEOUT ]; then
        echo "应用未能在$TIMEOUT秒内停止，将强制终止"
        kill -9 "$PID"
        break
    fi
done

# 删除PID文件
rm "$PID_FILE"
echo "AIKG应用已停止" 