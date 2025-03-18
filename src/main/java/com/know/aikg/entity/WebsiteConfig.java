package com.know.aikg.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 网站配置实体类，用于存储需要爬取的网站信息
 */
@Entity
@Table(name = "website_config")
public class WebsiteConfig {
    
    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 网站名称
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    
    /**
     * 网站URL
     */
    @Column(name = "url", nullable = false, length = 255)
    private String url;
    
    /**
     * 网站基础域名
     */
    @Column(name = "base_domain", nullable = false, length = 255)
    private String baseDomain;
    
    /**
     * 是否启用
     */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;
    
    /**
     * 今日爬取尝试次数
     */
    @Column(name = "today_retry_count", nullable = false)
    private Integer todayRetryCount = 0;
    
    /**
     * 最后一次爬取时间
     */
    @Column(name = "last_fetch_time")
    private LocalDateTime lastFetchTime;
    
    /**
     * 最后一次爬取状态：0-未爬取，1-爬取成功，2-爬取失败
     */
    @Column(name = "last_fetch_status", nullable = false)
    private Integer lastFetchStatus = 0;
    
    /**
     * 创建时间
     */
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;
    
    @PrePersist
    protected void onCreate() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updateTime = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public String getBaseDomain() {
        return baseDomain;
    }
    
    public void setBaseDomain(String baseDomain) {
        this.baseDomain = baseDomain;
    }
    
    public Boolean getEnabled() {
        return enabled;
    }
    
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
    
    public Integer getTodayRetryCount() {
        return todayRetryCount;
    }
    
    public void setTodayRetryCount(Integer todayRetryCount) {
        this.todayRetryCount = todayRetryCount;
    }
    
    public LocalDateTime getLastFetchTime() {
        return lastFetchTime;
    }
    
    public void setLastFetchTime(LocalDateTime lastFetchTime) {
        this.lastFetchTime = lastFetchTime;
    }
    
    public Integer getLastFetchStatus() {
        return lastFetchStatus;
    }
    
    public void setLastFetchStatus(Integer lastFetchStatus) {
        this.lastFetchStatus = lastFetchStatus;
    }
    
    public LocalDateTime getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
    
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }
    
    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
    
    @Override
    public String toString() {
        return "WebsiteConfig{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", url='" + url + '\'' +
                ", baseDomain='" + baseDomain + '\'' +
                ", enabled=" + enabled +
                ", todayRetryCount=" + todayRetryCount +
                ", lastFetchTime=" + lastFetchTime +
                ", lastFetchStatus=" + lastFetchStatus +
                '}';
    }
} 