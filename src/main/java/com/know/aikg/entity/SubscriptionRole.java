package com.know.aikg.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import org.hibernate.annotations.GenericGenerator;

/**
 * 订阅角色实体类
 * 
 * 表示系统中的一个订阅角色，包含订阅的领域、读者信息、状态等
 * 
 * @Entity: 标记该类为JPA实体
 * @Table: 指定对应的数据库表名
 */
@Entity
@Table(name = "t_subscription_role")
public class SubscriptionRole {
    
    /**
     * 主键ID
     * 
     * @Id: 标记为主键
     * @GeneratedValue: 使用UUID生成策略
     * @Column: 指定列的属性
     */
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "id", columnDefinition = "VARCHAR(36)")
    private String id;

    /**
     * 订阅领域
     * 
     * 表示订阅的知识领域或主题
     * 
     * @NotBlank: 不能为空白
     * @Size: 长度限制为2-100个字符
     * @Column: 对应数据库列的属性设置
     */
    @NotBlank(message = "领域不能为空")
    @Size(min = 2, max = 100, message = "领域长度必须在2-100个字符之间")
    @Column(nullable = false, length = 100)
    private String area;

    /**
     * 读者名称
     * 
     * 订阅者的名称或称呼
     * 
     * @NotBlank: 不能为空白
     * @Size: 长度限制为2-50个字符
     * @Column: 对应数据库列的属性设置
     */
    @NotBlank(message = "读者名称不能为空")
    @Size(min = 2, max = 50, message = "读者名称长度必须在2-50个字符之间")
    @Column(nullable = false, length = 50)
    private String reader;

    /**
     * 读者邮箱
     * 
     * 用于接收订阅内容的邮箱地址
     * 
     * @NotBlank: 不能为空白
     * @Email: 必须是有效的邮箱格式
     * @Column: 对应数据库列的属性设置
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Column(name = "reader_email", nullable = false, length = 100)
    private String readerEmail;

    /**
     * 订阅状态
     * 
     * true表示激活状态，false表示停用状态
     * 
     * @NotNull: 不能为null
     * @Column: 对应数据库列的属性设置
     */
    @NotNull(message = "状态不能为空")
    @Column(nullable = false)
    private Boolean status = true;

    /**
     * 创建时间
     * 
     * 记录订阅创建的时间戳
     * 
     * @Column: 对应数据库列的属性设置，设置为不可更新
     */
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    /**
     * 更新时间
     * 
     * 记录订阅最后一次更新的时间戳
     * 
     * @Column: 对应数据库列的属性设置
     */
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    /**
     * 定时任务表达式
     * 
     * 用于设置该订阅文章生成和邮件发送的时间
     * 使用cron表达式格式，例如"0 0 8 * * ?"表示每天早上8点
     * 
     * @Column: 对应数据库列的属性设置
     */
    @Column(name = "schedule_cron", length = 50)
    private String scheduleCron;

    /**
     * 实体创建前的回调方法
     * 
     * 自动设置创建时间和更新时间为当前时间
     * 
     * @PrePersist: 在实体持久化之前自动调用
     */
    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
    }

    /**
     * 实体更新前的回调方法
     * 
     * 自动更新更新时间为当前时间
     * 
     * @PreUpdate: 在实体更新之前自动调用
     */
    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }

    // Getters and Setters
    /**
     * 获取订阅ID
     * 
     * @return 订阅ID
     */
    public String getId() {
        return id;
    }

    /**
     * 设置订阅ID
     * 
     * @param id 订阅ID
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * 获取订阅领域
     * 
     * @return 订阅领域
     */
    public String getArea() {
        return area;
    }

    /**
     * 设置订阅领域
     * 
     * @param area 订阅领域
     */
    public void setArea(String area) {
        this.area = area;
    }

    /**
     * 获取读者名称
     * 
     * @return 读者名称
     */
    public String getReader() {
        return reader;
    }

    /**
     * 设置读者名称
     * 
     * @param reader 读者名称
     */
    public void setReader(String reader) {
        this.reader = reader;
    }

    /**
     * 获取读者邮箱
     * 
     * @return 读者邮箱
     */
    public String getReaderEmail() {
        return readerEmail;
    }

    /**
     * 设置读者邮箱
     * 
     * @param readerEmail 读者邮箱
     */
    public void setReaderEmail(String readerEmail) {
        this.readerEmail = readerEmail;
    }

    /**
     * 获取订阅状态
     * 
     * @return 订阅状态，true表示激活，false表示停用
     */
    public Boolean getStatus() {
        return status;
    }

    /**
     * 设置订阅状态
     * 
     * @param status 订阅状态，true表示激活，false表示停用
     */
    public void setStatus(Boolean status) {
        this.status = status;
    }

    /**
     * 获取创建时间
     * 
     * @return 创建时间
     */
    public LocalDateTime getCreateTime() {
        return createTime;
    }

    /**
     * 获取更新时间
     * 
     * @return 更新时间
     */
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    /**
     * 获取定时任务表达式
     * 
     * @return 定时任务表达式
     */
    public String getScheduleCron() {
        return scheduleCron;
    }

    /**
     * 设置定时任务表达式
     * 
     * @param scheduleCron 定时任务表达式
     */
    public void setScheduleCron(String scheduleCron) {
        this.scheduleCron = scheduleCron;
    }
} 