-- 添加定时任务表达式字段
ALTER TABLE `t_subscription_role` 
ADD COLUMN `schedule_cron` varchar(50) DEFAULT '0 0 8 * * ?' COMMENT '定时任务表达式，默认每天早上8点';

-- 更新现有数据，设置默认值
UPDATE `t_subscription_role` SET `schedule_cron` = '0 0 8 * * ?'; 