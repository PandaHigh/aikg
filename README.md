# AI知识生成器 (AIKG)

AIKG是一个基于Spring Boot的自动化知识文章生成系统。该系统利用AI技术生成特定主题的文章，并通过邮件定期发送给订阅者。

## 主要功能

- **AI内容生成**：使用大语言模型自动生成文章标题和内容
- **定时邮件发送**：自动向订阅者发送个性化文章
- **多主题支持**：目前支持以下主题：
  - AI/大语言模型前沿进展（面向程序员）
  - 母婴知识（面向孕妇）
- **系统监控**：每日向管理员发送系统心跳邮件
- **可配置的邮件设置**：支持多收件人和自定义发件人配置

## 技术栈

- Java 21
- Spring Boot 3.2.2
- Spring AI（OpenAI集成）
- Spring Mail

## 配置说明

### 配置文件

在`application.properties`中配置以下内容：

```properties
# 邮件配置
spring.mail.host=邮件服务器地址
spring.mail.port=587
spring.mail.username=邮箱账号
spring.mail.password=邮箱密码
spring.mail.from=发件人地址
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# OpenAI配置
spring.ai.openai.api-key=你的OpenAI密钥
spring.ai.openai.base-url=OpenAI接口地址
spring.ai.openai.chat.options.model=模型名称
spring.ai.openai.chat.options.max-tokens=2000
spring.ai.openai.client.connect-timeout=60s
spring.ai.openai.client.read-timeout=60s

# 邮件接收人配置
email.admin.recipients=管理员1邮箱,管理员2邮箱
```

## 核心组件

1. **AIService**: 负责调用AI接口生成内容
2. **EmailService**: 处理邮件发送功能
3. **ScheduledService**: 协调定时任务和内容生成
4. **RoleKeywords**: 定义不同内容类型和目标受众的枚举类

## 定时任务

1. **文章生成任务**: 定时生成并发送特定主题的文章给订阅者
2. **系统监控任务**: 每天早上8:30发送系统心跳邮件

## 使用说明

1. 克隆代码仓库
2. 配置`application.properties`
3. 构建项目：
   ```bash
   mvn clean install
   ```
4. 运行应用：
   ```bash
   java -jar target/aikg-0.0.1-SNAPSHOT.jar
   ```

## 添加新主题

在`RoleKeywords`枚举中添加新主题：

```java
NEW_TOPIC("专家角色", "领域方向", "目标读者", "订阅者邮箱")
```

## 系统监控

- 通过应用日志监控内容生成和邮件发送状态
- 通过每日心跳邮件监控系统健康状态
- 查看邮件服务器的发送日志

## 代码结构

```
src/main/java/com/know/aikg/
├── service/
│   ├── AIService.java          # AI服务类
│   ├── EmailService.java       # 邮件服务类
│   ├── ScheduledService.java   # 定时任务服务类
│   └── RoleKeywords.java       # 角色关键词枚举类
└── AikgApplication.java        # 应用程序入口
```

## 贡献指南

1. Fork 项目仓库
2. 创建特性分支
3. 提交代码变更
4. 推送到分支
5. 创建 Pull Request

## 注意事项

1. 确保OpenAI API密钥配置正确
2. 邮箱配置需要开启SMTP服务
3. 建议配置足够的超时时间以应对AI生成延迟
4. 定期检查系统心跳邮件确保服务正常运行

## 许可证

本项目采用 MIT 许可证 - 详见 LICENSE 文件 