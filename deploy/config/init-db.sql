-- AIKG 数据库初始化脚本

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS aikg CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建用户并授权
CREATE USER IF NOT EXISTS 'aikg_prod'@'localhost' IDENTIFIED BY 'StrongPassword123!';
GRANT ALL PRIVILEGES ON aikg.* TO 'aikg_prod'@'localhost';

-- 允许从应用服务器连接（如果数据库和应用不在同一台服务器上）
-- CREATE USER IF NOT EXISTS 'aikg_prod'@'%' IDENTIFIED BY 'StrongPassword123!';
-- GRANT ALL PRIVILEGES ON aikg.* TO 'aikg_prod'@'%';

-- 刷新权限
FLUSH PRIVILEGES;

-- 注意：数据库表已创建，无需再次创建表结构

-- 使用数据库
USE aikg;

-- 此处可以添加表结构和初始数据
-- 注意：如果使用JPA的自动创建表功能（spring.jpa.hibernate.ddl-auto=update），则不需要在此处创建表

-- 示例：创建用户表
-- CREATE TABLE IF NOT EXISTS `user` (
--   `id` bigint(20) NOT NULL AUTO_INCREMENT,
--   `username` varchar(50) NOT NULL,
--   `password` varchar(100) NOT NULL,
--   `email` varchar(100) DEFAULT NULL,
--   `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
--   `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
--   PRIMARY KEY (`id`),
--   UNIQUE KEY `uk_username` (`username`),
--   UNIQUE KEY `uk_email` (`email`)
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci; 