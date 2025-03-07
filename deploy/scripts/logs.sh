#!/bin/bash

# AIKG 应用日志查看脚本

# 设置脚本所在目录为工作目录
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
APP_DIR="$( cd "$SCRIPT_DIR/../.." && pwd )"
LOG_DIR="$APP_DIR/logs"
LOG_FILE="$LOG_DIR/aikg.log"

# 检查日志文件是否存在
if [ ! -f "$LOG_FILE" ]; then
    echo "日志文件不存在: $LOG_FILE"
    exit 1
fi

# 根据参数显示日志
if [ "$1" == "-f" ] || [ "$1" == "--follow" ]; then
    # 实时查看日志
    tail -f "$LOG_FILE"
elif [ "$1" == "-n" ] && [ -n "$2" ]; then
    # 查看最后n行日志
    tail -n "$2" "$LOG_FILE"
elif [ "$1" == "--error" ]; then
    # 只查看错误日志
    grep -i "error\|exception" "$LOG_FILE" | tail -n 100
elif [ "$1" == "--today" ]; then
    # 只查看今天的日志
    TODAY=$(date '+%Y-%m-%d')
    grep "$TODAY" "$LOG_FILE"
else
    # 默认显示最后100行日志
    tail -n 100 "$LOG_FILE"
fi 