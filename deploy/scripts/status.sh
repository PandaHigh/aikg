#!/bin/bash

# AIKG 应用状态检查脚本

# 设置脚本所在目录为工作目录
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
APP_DIR="$( cd "$SCRIPT_DIR/../.." && pwd )"
PID_FILE="$APP_DIR/aikg.pid"

# 检查PID文件是否存在
if [ ! -f "$PID_FILE" ]; then
    echo "状态: 未运行"
    echo "PID文件不存在，应用可能未启动"
    exit 1
fi

# 读取PID
PID=$(cat "$PID_FILE")

# 检查进程是否存在
if ! ps -p "$PID" > /dev/null; then
    echo "状态: 未运行"
    echo "PID文件存在，但进程不存在，PID: $PID"
    echo "应用可能已异常终止"
    exit 1
fi

# 获取进程信息
PROCESS_INFO=$(ps -p "$PID" -o pid,ppid,user,%cpu,%mem,vsz,rss,stat,start,time,command | tail -n 1)

# 检查应用健康状态
HEALTH_CHECK_URL="http://localhost:80/actuator/health"
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$HEALTH_CHECK_URL" 2>/dev/null)

if [ "$HTTP_STATUS" == "200" ]; then
    HEALTH_STATUS="健康"
else
    HEALTH_STATUS="不健康 (HTTP状态码: $HTTP_STATUS)"
fi

# 获取运行时间
START_TIME=$(ps -p "$PID" -o lstart= 2>/dev/null)
if [ -n "$START_TIME" ]; then
    START_SECONDS=$(date -d "$START_TIME" +%s 2>/dev/null)
    if [ $? -eq 0 ]; then
        CURRENT_SECONDS=$(date +%s)
        UPTIME_SECONDS=$((CURRENT_SECONDS - START_SECONDS))
        UPTIME_DAYS=$((UPTIME_SECONDS / 86400))
        UPTIME_HOURS=$(( (UPTIME_SECONDS % 86400) / 3600 ))
        UPTIME_MINUTES=$(( (UPTIME_SECONDS % 3600) / 60 ))
        UPTIME_SECONDS=$((UPTIME_SECONDS % 60))
        UPTIME="${UPTIME_DAYS}天 ${UPTIME_HOURS}小时 ${UPTIME_MINUTES}分钟 ${UPTIME_SECONDS}秒"
    else
        UPTIME="无法计算"
    fi
else
    UPTIME="无法获取"
fi

# 显示状态信息
echo "AIKG应用状态信息:"
echo "----------------------------------------"
echo "状态: 运行中"
echo "PID: $PID"
echo "健康状态: $HEALTH_STATUS"
echo "运行时间: $UPTIME"
echo "----------------------------------------"
echo "进程详情:"
ps -p "$PID" -o pid,ppid,user,%cpu,%mem,vsz,rss,stat,start,time,comm | head -n 1
echo "$PROCESS_INFO"
echo "----------------------------------------"

# 如果应用健康，返回状态码0，否则返回1
if [ "$HEALTH_STATUS" == "健康" ]; then
    exit 0
else
    exit 1
fi 