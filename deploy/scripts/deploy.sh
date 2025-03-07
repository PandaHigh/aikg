#!/bin/bash

# AIKG 应用快速部署脚本
# 此脚本用于快速部署应用到指定服务器

# 设置脚本所在目录为工作目录
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
APP_DIR="$( cd "$SCRIPT_DIR/../.." && pwd )"
DEPLOY_DIR="$APP_DIR/deploy"

# 默认配置
SERVER_USER="root"
SERVER_HOST="110.41.138.56"
DEPLOY_PATH="/opt/aikg"
PACKAGE_NAME="aikg-$(date +%Y%m%d).tar.gz"

# 显示帮助信息
show_help() {
    echo "AIKG 应用快速部署脚本"
    echo "用法: $0 [选项]"
    echo "选项:"
    echo "  -h, --help              显示此帮助信息"
    echo "  -u, --user USER         服务器用户名 (默认: $SERVER_USER)"
    echo "  -s, --server SERVER     服务器地址 (默认: $SERVER_HOST)"
    echo "  -p, --path PATH         部署路径 (默认: $DEPLOY_PATH)"
    echo "  --skip-build            跳过构建步骤"
    echo "  --skip-package          跳过打包步骤"
    echo "  --skip-upload           跳过上传步骤"
    echo "  --skip-check            跳过配置检查步骤"
    echo "  --restart               部署后重启应用"
    echo "示例:"
    echo "  $0 -u admin -s example.com -p /opt/aikg --restart"
}

# 解析命令行参数
SKIP_BUILD=false
SKIP_PACKAGE=false
SKIP_UPLOAD=false
SKIP_CHECK=false
RESTART=false

while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            exit 0
            ;;
        -u|--user)
            SERVER_USER="$2"
            shift 2
            ;;
        -s|--server)
            SERVER_HOST="$2"
            shift 2
            ;;
        -p|--path)
            DEPLOY_PATH="$2"
            shift 2
            ;;
        --skip-build)
            SKIP_BUILD=true
            shift
            ;;
        --skip-package)
            SKIP_PACKAGE=true
            shift
            ;;
        --skip-upload)
            SKIP_UPLOAD=true
            shift
            ;;
        --skip-check)
            SKIP_CHECK=true
            shift
            ;;
        --restart)
            RESTART=true
            shift
            ;;
        *)
            echo "未知选项: $1"
            show_help
            exit 1
            ;;
    esac
done

# 检查配置
if [ "$SKIP_CHECK" = false ]; then
    echo "=== 检查部署配置 ==="
    "$SCRIPT_DIR/check-config.sh"
    CHECK_STATUS=$?
    if [ $CHECK_STATUS -eq 1 ]; then
        echo "配置检查失败，退出部署"
        echo "请修复配置问题后重试，或使用 --skip-check 跳过检查"
        exit 1
    fi
else
    echo "=== 跳过配置检查步骤 ==="
fi

# 构建应用
if [ "$SKIP_BUILD" = false ]; then
    echo "=== 构建应用 ==="
    cd "$APP_DIR"
    ./mvnw clean package -DskipTests
    if [ $? -ne 0 ]; then
        echo "构建失败，退出部署"
        exit 1
    fi
    echo "构建成功"
else
    echo "=== 跳过构建步骤 ==="
fi

# 打包应用
if [ "$SKIP_PACKAGE" = false ]; then
    echo "=== 打包应用 ==="
    "$SCRIPT_DIR/package.sh"
    if [ $? -ne 0 ]; then
        echo "打包失败，退出部署"
        exit 1
    fi
    echo "打包成功"
else
    echo "=== 跳过打包步骤 ==="
fi

# 上传应用
if [ "$SKIP_UPLOAD" = false ]; then
    echo "=== 上传应用到服务器 ==="
    echo "上传到 $SERVER_USER@$SERVER_HOST:$DEPLOY_PATH"
    
    # 上传包
    scp "$APP_DIR/$PACKAGE_NAME" "$SERVER_USER@$SERVER_HOST:/tmp/"
    if [ $? -ne 0 ]; then
        echo "上传失败，退出部署"
        exit 1
    fi
    
    # 解压包
    ssh "$SERVER_USER@$SERVER_HOST" "mkdir -p $DEPLOY_PATH && tar -xzf /tmp/$PACKAGE_NAME -C $DEPLOY_PATH && rm /tmp/$PACKAGE_NAME"
    if [ $? -ne 0 ]; then
        echo "解压失败，退出部署"
        exit 1
    fi
    
    # 设置权限
    ssh "$SERVER_USER@$SERVER_HOST" "chmod +x $DEPLOY_PATH/deploy/scripts/*.sh"
    
    echo "上传成功"
else
    echo "=== 跳过上传步骤 ==="
fi

# 重启应用
if [ "$RESTART" = true ]; then
    echo "=== 重启应用 ==="
    ssh "$SERVER_USER@$SERVER_HOST" "$DEPLOY_PATH/deploy/scripts/restart.sh"
    if [ $? -ne 0 ]; then
        echo "重启失败"
        exit 1
    fi
    echo "重启成功"

fi

echo "=== 部署完成 ==="
echo "可以通过以下命令检查应用状态:"
echo "ssh $SERVER_USER@$SERVER_HOST \"$DEPLOY_PATH/deploy/scripts/status.sh\"" 