CREATE TABLE `t_subscription_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `area` varchar(100) NOT NULL COMMENT '领域',
  `reader` varchar(50) NOT NULL COMMENT '目标读者',
  `reader_email` varchar(100) NOT NULL COMMENT '读者邮箱',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态：1-启用，0-禁用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_email` (`reader_email`),
  KEY `idx_area` (`area`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订阅角色信息表';

-- 初始化数据
INSERT INTO `t_subscription_role` 
(`area`, `reader`, `reader_email`) 
VALUES 
('大语言模型前沿进展', '程序员', 'xinlongzhan@webank.com'),
('母婴知识', '孕妇', 'xinlongzhan@webank.com'); 