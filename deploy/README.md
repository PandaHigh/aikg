# AIKG 部署指南

本目录包含将AIKG应用部署到云服务器的所有必要脚本和配置文件。

## 目录结构

- `scripts/` - 包含启动、停止和监控应用的脚本
- `config/` - 包含服务配置文件
- `env/` - 包含环境变量配置文件

## 部署步骤

1. 在服务器上安装必要的软件：
   - JDK 17
   - MySQL 8.0
   - Nginx (可选，用于反向代理)

2. 将应用程序JAR文件和部署目录上传到服务器

3. 配置环境变量：
   ```bash
   cp env/env.example env/.env
   # 编辑 env/.env 文件，设置必要的环境变量
   ```

4. 配置数据库：
   ```bash
   mysql -u root -p < config/init-db.sql
   ```

5. 配置系统服务：
   ```bash
   sudo cp config/aikg.service /etc/systemd/system/
   sudo systemctl daemon-reload
   ```

6. 启动应用：
   ```bash
   sudo systemctl start aikg
   ```

7. 设置开机自启：
   ```bash
   sudo systemctl enable aikg
   ```

## 管理应用

- 启动应用：`./scripts/start.sh` 或 `sudo systemctl start aikg`
- 停止应用：`./scripts/stop.sh` 或 `sudo systemctl stop aikg`
- 重启应用：`./scripts/restart.sh` 或 `sudo systemctl restart aikg`
- 查看日志：`./scripts/logs.sh` 或 `journalctl -u aikg`
- 检查状态：`./scripts/status.sh` 或 `sudo systemctl status aikg`

## 监控和维护

应用程序配置了健康检查端点，可以通过以下方式监控：

```bash
curl http://localhost:8080/actuator/health
```

建议设置定时任务定期检查应用状态，并在出现问题时自动重启：

```bash
*/5 * * * * /path/to/aikg/deploy/scripts/monitor.sh >> /path/to/aikg/logs/monitor.log 2>&1
``` 