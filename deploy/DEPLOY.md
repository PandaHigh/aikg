# AIKG 应用部署指南

本文档提供了将AIKG应用部署到云服务器的详细步骤。

## 前置条件

- 一台运行Linux的云服务器（推荐Ubuntu 20.04/22.04或CentOS 7/8）
- 至少2GB内存
- 至少20GB磁盘空间
- 已安装以下软件：
  - JDK 17
  - MySQL 8.0
  - Git (可选)

## 部署步骤

### 1. 准备服务器环境

#### 安装JDK 17

Ubuntu:
```bash
sudo apt update
sudo apt install -y openjdk-17-jdk
```

CentOS:
```bash
sudo yum install -y java-17-openjdk-devel
```

验证安装:
```bash
java -version
```

#### 安装MySQL 8.0

Ubuntu:
```bash
sudo apt update
sudo apt install -y mysql-server
sudo systemctl start mysql
sudo systemctl enable mysql
```

CentOS:
```bash
sudo yum install -y https://dev.mysql.com/get/mysql80-community-release-el7-3.noarch.rpm
sudo yum install -y mysql-community-server
sudo systemctl start mysqld
sudo systemctl enable mysqld
```

设置MySQL root密码:
```bash
sudo mysql_secure_installation
```

### 2. 创建应用用户

```bash
sudo useradd -m -s /bin/bash aikg
sudo groupadd aikg
sudo usermod -aG aikg aikg
```

### 3. 准备应用目录

```bash
sudo mkdir -p /opt/aikg
sudo chown -R aikg:aikg /opt/aikg
```

### 4. 构建应用

在开发机器上:

```bash
# 克隆代码库（如果使用Git）
git clone <repository-url> aikg
cd aikg

# 构建应用
./mvnw clean package -DskipTests
```

### 5. 上传应用到服务器

使用scp或rsync将应用上传到服务器:

```bash
# 创建一个包含必要文件的部署包
mkdir -p deploy_package
cp -r target/aikg-0.0.1-SNAPSHOT.jar deploy_package/
cp -r deploy deploy_package/
cp -r logs deploy_package/

# 上传到服务器
scp -r deploy_package/* user@your-server:/opt/aikg/
```

### 6. 配置应用

在服务器上:

```bash
# 切换到aikg用户
sudo su - aikg

# 进入应用目录
cd /opt/aikg

# 设置脚本执行权限
chmod +x deploy/scripts/*.sh

# 配置环境变量
cp deploy/env/env.example deploy/env/.env
nano deploy/env/.env  # 编辑环境变量
```

### 7. 配置数据库用户和权限

注意：数据库表已创建，只需配置用户和权限

```bash
# 使用MySQL root用户执行初始化脚本
sudo mysql -u root -p < deploy/config/init-db.sql
```

### 8. 配置系统服务

```bash
# 复制服务配置文件
sudo cp deploy/config/aikg.service /etc/systemd/system/

# 重新加载systemd配置
sudo systemctl daemon-reload
```

### 9. 启动应用

```bash
# 使用systemd启动服务
sudo systemctl start aikg

# 设置开机自启
sudo systemctl enable aikg

# 检查服务状态
sudo systemctl status aikg
```

### 10. 配置防火墙（可选）

如果服务器启用了防火墙，需要开放应用端口:

Ubuntu (UFW):
```bash
sudo ufw allow 8080/tcp
```

CentOS (Firewalld):
```bash
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --reload
```

### 11. 配置Nginx反向代理（可选）

如果需要使用域名访问应用或启用HTTPS，可以配置Nginx反向代理:

安装Nginx:
```bash
# Ubuntu
sudo apt install -y nginx

# CentOS
sudo yum install -y nginx
```

配置Nginx:
```bash
sudo nano /etc/nginx/sites-available/aikg
```

添加以下内容:
```
server {
    listen 80;
    server_name your-domain.com;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

启用配置:
```bash
# Ubuntu
sudo ln -s /etc/nginx/sites-available/aikg /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx

# CentOS
sudo cp /etc/nginx/sites-available/aikg /etc/nginx/conf.d/
sudo nginx -t
sudo systemctl restart nginx
```

### 12. 配置HTTPS（可选）

使用Let's Encrypt配置HTTPS:

```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d your-domain.com
```

## 配置说明

本部署方案直接使用项目中的 `application.properties` 作为生产配置文件，无需额外的配置文件。如需修改配置，可以通过以下方式：

1. **环境变量覆盖**：在 `deploy/env/.env` 文件中设置环境变量，这些变量会在启动时被加载
2. **命令行参数**：如需临时修改配置，可以在 `start.sh` 脚本中的 `APP_OPTS` 变量中添加命令行参数
3. **直接修改配置文件**：如果需要永久修改配置，可以直接编辑 `src/main/resources/application.properties` 文件

## 维护应用

### 日常维护

- 查看应用状态: `sudo systemctl status aikg` 或 `/opt/aikg/deploy/scripts/status.sh`
- 查看应用日志: `sudo journalctl -u aikg` 或 `/opt/aikg/deploy/scripts/logs.sh`
- 重启应用: `sudo systemctl restart aikg` 或 `/opt/aikg/deploy/scripts/restart.sh`

### 设置监控

配置crontab定时任务监控应用状态:

```bash
sudo crontab -e
```

添加以下内容:
```
*/5 * * * * /opt/aikg/deploy/scripts/monitor.sh >> /opt/aikg/logs/monitor.log 2>&1
```

### 更新应用

1. 构建新版本的应用
2. 停止当前运行的应用: `sudo systemctl stop aikg`
3. 备份当前版本: `cp /opt/aikg/target/aikg-0.0.1-SNAPSHOT.jar /opt/aikg/backup/aikg-$(date +%Y%m%d).jar`
4. 上传新版本: `scp target/aikg-0.0.1-SNAPSHOT.jar user@your-server:/opt/aikg/target/`
5. 启动应用: `sudo systemctl start aikg`
6. 验证应用状态: `sudo systemctl status aikg`

## 故障排除

### 应用无法启动

1. 检查日志: `sudo journalctl -u aikg` 或 `cat /opt/aikg/logs/aikg.log`
2. 检查配置: 确保环境变量和应用配置正确
3. 检查数据库连接: 确保数据库服务正常运行，且应用可以连接到数据库
4. 检查权限: 确保应用目录和文件的所有者是aikg用户

### 应用运行缓慢

1. 检查系统资源: `top`, `free -m`, `df -h`
2. 检查JVM内存设置: 调整`deploy/env/.env`中的`JVM_XMS`和`JVM_XMX`参数
3. 检查数据库性能: 使用MySQL性能工具分析数据库性能

### 应用崩溃

1. 检查日志中的错误信息
2. 检查系统资源是否不足
3. 确保监控脚本正常运行，可以自动重启应用

## 联系支持

如有任何问题，请联系技术支持团队:
- 邮箱: support@example.com
- 电话: 123-456-7890 