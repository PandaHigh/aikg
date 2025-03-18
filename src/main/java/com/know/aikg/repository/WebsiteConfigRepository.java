package com.know.aikg.repository;

import com.know.aikg.entity.WebsiteConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 网站配置存储库接口
 */
@Repository
public interface WebsiteConfigRepository extends JpaRepository<WebsiteConfig, Long> {
    
    /**
     * 查找所有启用的网站配置
     * @return 网站配置列表
     */
    List<WebsiteConfig> findByEnabledTrue();
    
    /**
     * 查找启用且今日重试次数小于最大重试次数的网站配置
     * @param maxRetryCount 最大重试次数
     * @return 符合条件的网站配置列表
     */
    @Query("SELECT w FROM WebsiteConfig w WHERE w.enabled = true AND w.todayRetryCount < :maxRetryCount")
    List<WebsiteConfig> findEnabledWebsitesWithRetryAvailable(int maxRetryCount);
    
    /**
     * 查找上次爬取失败且今日重试次数小于最大重试次数的网站配置
     * @param maxRetryCount 最大重试次数
     * @return 符合条件的网站配置列表
     */
    @Query("SELECT w FROM WebsiteConfig w WHERE w.enabled = true AND w.lastFetchStatus = 2 AND w.todayRetryCount < :maxRetryCount")
    List<WebsiteConfig> findFailedWebsitesWithRetryAvailable(int maxRetryCount);
    
    /**
     * 查找启用且当天未成功爬取的网站
     * 条件：启用 AND (未爬取 OR 爬取失败) AND 今日重试次数小于最大重试次数
     * @param maxRetryCount 最大重试次数
     * @return 符合条件的网站配置列表
     */
    @Query("SELECT w FROM WebsiteConfig w WHERE w.enabled = true AND (w.lastFetchStatus = 0 OR w.lastFetchStatus = 2) AND w.todayRetryCount < :maxRetryCount")
    List<WebsiteConfig> findPendingWebsites(int maxRetryCount);
    
    /**
     * 重置所有网站的今日爬取尝试次数
     */
    @Modifying
    @Query("UPDATE WebsiteConfig w SET w.todayRetryCount = 0")
    void resetAllTodayRetryCount();
    
    /**
     * 更新网站的爬取状态
     * @param id 网站配置ID
     * @param status 爬取状态（1-成功，2-失败）
     * @param fetchTime 爬取时间
     */
    @Modifying
    @Query("UPDATE WebsiteConfig w SET w.lastFetchStatus = :status, w.lastFetchTime = :fetchTime, w.todayRetryCount = w.todayRetryCount + 1 WHERE w.id = :id")
    void updateFetchStatus(Long id, Integer status, LocalDateTime fetchTime);
} 