#!/bin/bash

# AIKG 应用打包脚本
# 此脚本用于将应用打包成可部署的格式

# 设置脚本所在目录为工作目录
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
APP_DIR="$( cd "$SCRIPT_DIR/../.." && pwd )"
DEPLOY_DIR="$APP_DIR/deploy"
TARGET_DIR="$APP_DIR/target"
PACKAGE_DIR="$APP_DIR/package"
JAR_FILE="$TARGET_DIR/aikg-0.0.1-SNAPSHOT.jar"
PACKAGE_NAME="aikg-$(date +%Y%m%d).tar.gz"

# 检查JAR文件是否存在
if [ ! -f "$JAR_FILE" ]; then
    echo "JAR文件不存在: $JAR_FILE"
    echo "请先构建应用"
    echo "运行: ./mvnw clean package -DskipTests"
    exit 1
fi

# 创建打包目录
echo "创建打包目录..."
rm -rf "$PACKAGE_DIR"
mkdir -p "$PACKAGE_DIR/target"
mkdir -p "$PACKAGE_DIR/logs"
mkdir -p "$PACKAGE_DIR/backup"

# 复制文件
echo "复制文件..."
cp "$JAR_FILE" "$PACKAGE_DIR/target/"
cp -r "$DEPLOY_DIR" "$PACKAGE_DIR/"
cp "$APP_DIR/README.md" "$PACKAGE_DIR/" 2>/dev/null || :

# 设置脚本执行权限
echo "设置脚本执行权限..."
chmod +x "$PACKAGE_DIR/deploy/scripts/"*.sh

# 创建打包文件
echo "创建打包文件..."
cd "$APP_DIR"
tar -czf "$PACKAGE_NAME" -C "$PACKAGE_DIR" .

# 清理临时目录
echo "清理临时目录..."
rm -rf "$PACKAGE_DIR"

echo "打包完成: $APP_DIR/$PACKAGE_NAME"