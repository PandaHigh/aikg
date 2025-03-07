package com.know.aikg.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

/**
 * 邮件服务类
 * 
 * 负责发送邮件，包括单个邮件和批量邮件发送功能
 * 使用Spring的JavaMailSender实现邮件发送
 * 
 * @Service: 标记该类为Spring服务组件
 */
@Service
public class EmailService {

    /**
     * 日志记录器
     */
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    /**
     * Spring邮件发送器
     * 用于发送邮件
     */
    @Autowired
    private JavaMailSender javaMailSender;
    
    /**
     * 邮件发送者地址
     * 从配置文件中注入
     */
    @Value("${spring.mail.from}")
    private String fromAddress;
    
    /**
     * 发送单个邮件
     * 
     * 向指定收件人发送一封邮件
     * 
     * @param to 收件人邮箱地址
     * @param subject 邮件主题
     * @param content 邮件内容
     * @throws RuntimeException 当邮件发送失败时抛出异常
     */
    public void sendEmail(String to, String subject, String content) {
        logger.info("准备发送邮件，接收者: {}, 主题: {}", to, subject);
        try {
            // 创建邮件消息对象
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            
            logger.debug("邮件内容长度: {} 字符", content.length());
            // 发送邮件
            javaMailSender.send(message);
            logger.info("邮件发送成功，接收者: {}", to);
        } catch (Exception e) {
            // 记录发送失败信息
            logger.error("邮件发送失败，接收者: {}, 错误: {}", to, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 批量发送邮件
     * 
     * 向多个收件人发送相同内容的邮件
     * 
     * @param toList 收件人邮箱地址数组
     * @param subject 邮件主题
     * @param content 邮件内容
     * @throws RuntimeException 当邮件发送失败时抛出异常
     */
    public void sendBatchEmail(String[] toList, String subject, String content) {
        logger.info("准备批量发送邮件，接收者数量: {}, 主题: {}", toList.length, subject);
        try {
            // 创建邮件消息对象
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toList);
            message.setSubject(subject);
            message.setText(content);
            
            logger.debug("邮件内容长度: {} 字符", content.length());
            // 发送邮件
            javaMailSender.send(message);
            logger.info("批量邮件发送成功，接收者数量: {}", toList.length);
        } catch (Exception e) {
            // 记录发送失败信息
            logger.error("批量邮件发送失败，接收者数量: {}, 错误: {}", toList.length, e.getMessage(), e);
            throw e;
        }
    }
} 