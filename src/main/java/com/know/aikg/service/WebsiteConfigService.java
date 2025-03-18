package com.know.aikg.service;

import com.know.aikg.entity.WebsiteConfig;

import java.util.List;
import java.util.Optional;

/**
 * 网站配置服务接口
 */
public interface WebsiteConfigService {
    
    /**
     * 查找所有启用的网站配置
     * @return 网站配置列表
     */
    List<WebsiteConfig> findAllEnabled();
    
    /**
     * 查找启用且今日重试次数小于最大重试次数的网站配置
     * @return 符合条件的网站配置列表
     */
    List<WebsiteConfig> findEnabledWithRetryAvailable();
    
    /**
     * 查找上次爬取失败且今日重试次数小于最大重试次数的网站配置
     * @return 符合条件的网站配置列表
     */
    List<WebsiteConfig> findFailedWithRetryAvailable();
    
    /**
     * 查找启用且当天未成功爬取的网站
     * @return 符合条件的网站配置列表
     */
    List<WebsiteConfig> findPendingWebsites();
    
    /**
     * 根据ID查找网站配置
     * @param id 网站配置ID
     * @return 网站配置对象
     */
    Optional<WebsiteConfig> findById(Long id);
    
    /**
     * 保存网站配置
     * @param websiteConfig 网站配置对象
     * @return 保存后的网站配置对象
     */
    WebsiteConfig save(WebsiteConfig websiteConfig);
    
    /**
     * 更新网站的爬取状态为成功
     * @param id 网站配置ID
     */
    void updateToSuccess(Long id);
    
    /**
     * 更新网站的爬取状态为失败
     * @param id 网站配置ID
     */
    void updateToFailed(Long id);
    
    /**
     * 重置所有网站的今日爬取尝试次数
     */
    void resetAllTodayRetryCount();
} 