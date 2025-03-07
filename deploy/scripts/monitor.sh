#!/bin/bash

# AIKG 应用监控脚本
# 建议通过crontab定时执行此脚本，例如：
# */5 * * * * /path/to/aikg/deploy/scripts/monitor.sh >> /path/to/aikg/logs/monitor.log 2>&1

# 设置脚本所在目录为工作目录
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
APP_DIR="$( cd "$SCRIPT_DIR/../.." && pwd )"
LOG_DIR="$APP_DIR/logs"
PID_FILE="$APP_DIR/aikg.pid"
MONITOR_LOG="$LOG_DIR/monitor.log"

# 创建日志目录（如果不存在）
mkdir -p "$LOG_DIR"

# 记录时间戳
echo "$(date '+%Y-%m-%d %H:%M:%S') - 开始监控AIKG应用..."

# 检查PID文件是否存在
if [ ! -f "$PID_FILE" ]; then
    echo "PID文件不存在，应用可能未运行，尝试启动应用..."
    "$SCRIPT_DIR/start.sh"
    exit 0
fi

# 读取PID
PID=$(cat "$PID_FILE")

# 检查进程是否存在
if ! ps -p "$PID" > /dev/null; then
    echo "进程不存在，PID: $PID，尝试重新启动应用..."
    rm -f "$PID_FILE"
    "$SCRIPT_DIR/start.sh"
    exit 0
fi

# 检查应用健康状态
HEALTH_CHECK_URL="http://localhost:8080/actuator/health"
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$HEALTH_CHECK_URL")

if [ "$HTTP_STATUS" != "200" ]; then
    echo "健康检查失败，HTTP状态码: $HTTP_STATUS，尝试重启应用..."
    "$SCRIPT_DIR/restart.sh"
    exit 0
fi

# 检查内存使用情况
MEM_USAGE=$(ps -o %mem -p "$PID" | tail -n 1 | tr -d ' ')
if (( $(echo "$MEM_USAGE > 80.0" | bc -l) )); then
    echo "内存使用率过高: ${MEM_USAGE}%，尝试重启应用..."
    "$SCRIPT_DIR/restart.sh"
    exit 0
fi

# 检查CPU使用情况
CPU_USAGE=$(ps -o %cpu -p "$PID" | tail -n 1 | tr -d ' ')
if (( $(echo "$CPU_USAGE > 90.0" | bc -l) )); then
    echo "CPU使用率过高: ${CPU_USAGE}%，尝试重启应用..."
    "$SCRIPT_DIR/restart.sh"
    exit 0
fi

# 应用运行正常
echo "AIKG应用运行正常，PID: $PID，内存使用率: ${MEM_USAGE}%，CPU使用率: ${CPU_USAGE}%" 