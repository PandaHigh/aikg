-- 创建网站配置表
CREATE TABLE IF NOT EXISTS website_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '网站名称',
    url VARCHAR(255) NOT NULL COMMENT '网站URL',
    base_domain VARCHAR(255) NOT NULL COMMENT '网站基础域名',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
    today_retry_count INT NOT NULL DEFAULT 0 COMMENT '今日爬取尝试次数',
    last_fetch_time DATETIME COMMENT '最后一次爬取时间',
    last_fetch_status INT NOT NULL DEFAULT 0 COMMENT '最后一次爬取状态：0-未爬取，1-爬取成功，2-爬取失败',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY idx_url (url)
) COMMENT='网站配置表';

-- 初始化一条记录，使用之前配置中的网站
INSERT INTO website_config (
    name, 
    url, 
    base_domain, 
    enabled, 
    today_retry_count,
    last_fetch_status,
    create_time,
    update_time
) VALUES (
    '默认网站', 
    'https://qh.sz.gov.cn/sygnan/xxgk/xxgkml/zcfg/zzwj/',
    'https://qh.sz.gov.cn',
    TRUE,
    0,
    0,
    NOW(),
    NOW()
); 


-- 初始化一条记录，使用之前配置中的网站
INSERT INTO website_config (
    name, 
    url, 
    base_domain, 
    enabled, 
    today_retry_count,
    last_fetch_status,
    create_time,
    update_time
) VALUES (
    '深圳税务局', 
    'https://shenzhen.chinatax.gov.cn/sztax/xxgk/xxgk.shtml',
    'https://shenzhen.chinatax.gov.cn',
    TRUE,
    0,
    0,
    NOW(),
    NOW()
); 