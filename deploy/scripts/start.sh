#!/bin/bash

# AIKG 应用启动脚本

# 设置脚本所在目录为工作目录
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
APP_DIR="$( cd "$SCRIPT_DIR/../.." && pwd )"
DEPLOY_DIR="$APP_DIR/deploy"
LOG_DIR="$APP_DIR/logs"
JAR_FILE="$APP_DIR/target/aikg-0.0.1-SNAPSHOT.jar"
PID_FILE="$APP_DIR/aikg.pid"
ENV_FILE="$DEPLOY_DIR/env/.env"

# 创建日志目录（如果不存在）
mkdir -p "$LOG_DIR"

# 加载环境变量
if [ -f "$ENV_FILE" ]; then
    source "$ENV_FILE"
else
    echo "环境变量文件不存在: $ENV_FILE"
    echo "请先创建环境变量文件"
    exit 1
fi

# 检查JAR文件是否存在
if [ ! -f "$JAR_FILE" ]; then
    echo "JAR文件不存在: $JAR_FILE"
    echo "请先构建应用"
    exit 1
fi

# 检查应用是否已经在运行
if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if ps -p "$PID" > /dev/null; then
        echo "AIKG应用已经在运行，PID: $PID"
        exit 0
    else
        echo "发现过期的PID文件，将删除"
        rm "$PID_FILE"
    fi
fi

# 设置JVM参数
JAVA_OPTS="-Xms${JVM_XMS:-512m} -Xmx${JVM_XMX:-1024m} -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=$LOG_DIR"

# 设置应用参数 - 直接使用application.properties配置文件
APP_OPTS=""

# 启动应用
echo "正在启动AIKG应用..."
nohup java $JAVA_OPTS -jar "$JAR_FILE" $APP_OPTS > "$LOG_DIR/aikg.log" 2>&1 &

# 保存PID
echo $! > "$PID_FILE"
echo "AIKG应用已启动，PID: $(cat "$PID_FILE")"

# 等待应用启动
echo "等待应用启动..."
sleep 10

# 检查应用是否成功启动
if ps -p $(cat "$PID_FILE") > /dev/null; then
    echo "AIKG应用启动成功！"
    echo "可以通过以下命令查看日志："
    echo "tail -f $LOG_DIR/aikg.log"
else
    echo "AIKG应用启动失败，请检查日志："
    echo "tail -f $LOG_DIR/aikg.log"
    exit 1
fi 