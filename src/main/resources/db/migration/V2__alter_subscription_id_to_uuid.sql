-- 1. 创建临时表，使用新的UUID结构
CREATE TABLE t_subscription_role_temp (
    id VARCHAR(36) NOT NULL,
    area VARCHAR(100) NOT NULL,
    reader VARCHAR(50) NOT NULL,
    reader_email VARCHAR(100) NOT NULL,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    create_time TIMESTAMP NOT NULL,
    update_time TIMESTAMP,
    schedule_cron VARCHAR(50),
    PRIMARY KEY (id)
);

-- 2. 将现有数据迁移到临时表，为每条记录生成UUID
INSERT INTO t_subscription_role_temp (
    id,
    area,
    reader,
    reader_email,
    status,
    create_time,
    update_time,
    schedule_cron
)
SELECT 
    REPLACE(UUID(), '-', ''),  -- 生成UUID并移除破折号
    area,
    reader,
    reader_email,
    status,
    create_time,
    update_time,
    schedule_cron
FROM t_subscription_role;

-- 3. 删除原表
DROP TABLE t_subscription_role;

-- 4. 将临时表重命名为原表名
RENAME TABLE t_subscription_role_temp TO t_subscription_role;

-- 5. 创建必要的索引
CREATE INDEX idx_subscription_reader_email ON t_subscription_role(reader_email);
CREATE INDEX idx_subscription_status ON t_subscription_role(status);
CREATE INDEX idx_subscription_area ON t_subscription_role(area); 