#!/bin/bash

# AIKG 应用配置检查脚本
# 此脚本用于检查部署环境的配置是否正确

# 设置脚本所在目录为工作目录
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
APP_DIR="$( cd "$SCRIPT_DIR/../.." && pwd )"
DEPLOY_DIR="$APP_DIR/deploy"
ENV_FILE="$DEPLOY_DIR/env/.env"
JAR_FILE="$APP_DIR/target/aikg-0.0.1-SNAPSHOT.jar"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
NC='\033[0m' # No Color

# 检查结果计数
PASS_COUNT=0
WARN_COUNT=0
FAIL_COUNT=0

# 显示标题
echo "====================================================="
echo "          AIKG 应用部署环境配置检查                  "
echo "====================================================="
echo "检查时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "检查目录: $APP_DIR"
echo "====================================================="

# 检查函数
check() {
    local name="$1"
    local command="$2"
    local expected_status="$3"
    
    echo -n "检查 $name... "
    
    eval "$command" > /dev/null 2>&1
    local status=$?
    
    if [ $status -eq $expected_status ]; then
        echo -e "${GREEN}通过${NC}"
        PASS_COUNT=$((PASS_COUNT + 1))
        return 0
    else
        echo -e "${RED}失败${NC}"
        FAIL_COUNT=$((FAIL_COUNT + 1))
        return 1
    fi
}

# 警告函数
warn() {
    local name="$1"
    local message="$2"
    
    echo -n "检查 $name... "
    echo -e "${YELLOW}警告${NC}"
    echo -e "${YELLOW}  $message${NC}"
    WARN_COUNT=$((WARN_COUNT + 1))
}

# 1. 检查JAR文件
check "JAR文件" "[ -f \"$JAR_FILE\" ]" 0
if [ $? -ne 0 ]; then
    echo "  错误: JAR文件不存在: $JAR_FILE"
    echo "  解决方案: 请先构建应用或检查JAR文件路径"
fi

# 2. 检查环境变量文件
check "环境变量文件" "[ -f \"$ENV_FILE\" ]" 0
if [ $? -ne 0 ]; then
    echo "  错误: 环境变量文件不存在: $ENV_FILE"
    echo "  解决方案: 请先创建环境变量文件: cp $DEPLOY_DIR/env/env.example $ENV_FILE"
else
    # 检查环境变量文件中的关键配置
    source "$ENV_FILE"
    
    # 检查数据库配置
    if [ -z "$DB_USER" ] || [ -z "$DB_PASSWORD" ]; then
        warn "数据库凭据" "数据库用户名或密码未设置"
        echo "  解决方案: 在环境变量文件中设置 DB_USER 和 DB_PASSWORD"
    fi
    
    # 检查OpenAI API配置
    if [ -z "$OPENAI_API_KEY" ] || [ "$OPENAI_API_KEY" == "your-api-key-here" ]; then
        warn "OpenAI API密钥" "OpenAI API密钥未设置或使用默认值"
        echo "  解决方案: 在环境变量文件中设置有效的 OPENAI_API_KEY"
    fi
    
    # 检查JVM配置
    if [ -z "$JVM_XMS" ] || [ -z "$JVM_XMX" ]; then
        warn "JVM内存配置" "JVM内存配置未设置"
        echo "  解决方案: 在环境变量文件中设置 JVM_XMS 和 JVM_XMX"
    fi
fi

# 3. 检查脚本执行权限
check "脚本执行权限" "[ -x \"$SCRIPT_DIR/start.sh\" ] && [ -x \"$SCRIPT_DIR/stop.sh\" ]" 0
if [ $? -ne 0 ]; then
    echo "  错误: 脚本没有执行权限"
    echo "  解决方案: chmod +x $SCRIPT_DIR/*.sh"
fi

# 4. 检查MySQL连接
if command -v mysql > /dev/null 2>&1; then
    if [ -n "$DB_USER" ] && [ -n "$DB_PASSWORD" ]; then
        check "MySQL连接" "mysql -u$DB_USER -p$DB_PASSWORD -h${DB_HOST:-localhost} -P${DB_PORT:-3306} -e 'SELECT 1' ${DB_NAME:-aikg}" 0
        if [ $? -ne 0 ]; then
            echo "  错误: 无法连接到MySQL数据库"
            echo "  解决方案: 检查数据库凭据和连接信息"
        fi
    else
        warn "MySQL连接" "未设置数据库凭据，跳过MySQL连接检查"
    fi
else
    warn "MySQL客户端" "MySQL客户端未安装，跳过MySQL连接检查"
    echo "  解决方案: 安装MySQL客户端以启用此检查"
fi

# 5. 检查Java版本
if command -v java > /dev/null 2>&1; then
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
    if [[ "$JAVA_VERSION" == 17* ]]; then
        echo -n "检查 Java版本... "
        echo -e "${GREEN}通过${NC} (版本: $JAVA_VERSION)"
        PASS_COUNT=$((PASS_COUNT + 1))
    else
        echo -n "检查 Java版本... "
        echo -e "${YELLOW}警告${NC} (版本: $JAVA_VERSION)"
        echo -e "${YELLOW}  应用需要Java 17，但当前版本是 $JAVA_VERSION${NC}"
        echo "  解决方案: 安装Java 17"
        WARN_COUNT=$((WARN_COUNT + 1))
    fi
else
    check "Java安装" "command -v java" 0
    if [ $? -ne 0 ]; then
        echo "  错误: Java未安装"
        echo "  解决方案: 安装JDK 17"
    fi
fi

# 6. 检查端口占用
if command -v netstat > /dev/null 2>&1 || command -v ss > /dev/null 2>&1; then
    PORT=${SERVER_PORT:-8080}
    if command -v netstat > /dev/null 2>&1; then
        PORT_CHECK=$(netstat -tuln | grep ":$PORT ")
    else
        PORT_CHECK=$(ss -tuln | grep ":$PORT ")
    fi
    
    if [ -n "$PORT_CHECK" ]; then
        echo -n "检查 端口占用... "
        echo -e "${YELLOW}警告${NC} (端口 $PORT 已被占用)"
        echo -e "${YELLOW}  端口 $PORT 已被其他进程占用${NC}"
        echo "  解决方案: 更改应用端口或停止占用该端口的进程"
        WARN_COUNT=$((WARN_COUNT + 1))
    else
        echo -n "检查 端口占用... "
        echo -e "${GREEN}通过${NC} (端口 $PORT 可用)"
        PASS_COUNT=$((PASS_COUNT + 1))
    fi
else
    warn "端口检查" "netstat和ss命令都不可用，跳过端口检查"
    echo "  解决方案: 安装net-tools或iproute2以启用此检查"
fi

# 7. 检查磁盘空间
check "磁盘空间" "[ $(df -P $APP_DIR | awk 'NR==2 {print $5}' | tr -d '%') -lt 90 ]" 0
if [ $? -ne 0 ]; then
    echo "  错误: 磁盘空间不足"
    echo "  解决方案: 清理磁盘空间或扩展磁盘容量"
fi

# 8. 检查内存
if command -v free > /dev/null 2>&1; then
    TOTAL_MEM=$(free -m | awk 'NR==2 {print $2}')
    if [ $TOTAL_MEM -lt 2048 ]; then
        echo -n "检查 系统内存... "
        echo -e "${YELLOW}警告${NC} (可用内存: ${TOTAL_MEM}MB)"
        echo -e "${YELLOW}  系统内存小于推荐值2GB${NC}"
        echo "  解决方案: 增加系统内存或减少JVM内存设置"
        WARN_COUNT=$((WARN_COUNT + 1))
    else
        echo -n "检查 系统内存... "
        echo -e "${GREEN}通过${NC} (可用内存: ${TOTAL_MEM}MB)"
        PASS_COUNT=$((PASS_COUNT + 1))
    fi
else
    warn "内存检查" "free命令不可用，跳过内存检查"
    echo "  解决方案: 安装procps以启用此检查"
fi

# 显示检查结果摘要
echo "====================================================="
echo "检查结果摘要:"
echo -e "${GREEN}通过: $PASS_COUNT${NC}"
echo -e "${YELLOW}警告: $WARN_COUNT${NC}"
echo -e "${RED}失败: $FAIL_COUNT${NC}"
echo "====================================================="

# 根据检查结果返回状态码
if [ $FAIL_COUNT -gt 0 ]; then
    echo -e "${RED}配置检查失败，请修复上述问题后再继续部署${NC}"
    exit 1
elif [ $WARN_COUNT -gt 0 ]; then
    echo -e "${YELLOW}配置检查完成，但有警告需要注意${NC}"
    exit 0
else
    echo -e "${GREEN}配置检查通过，可以继续部署${NC}"
    exit 0
fi 