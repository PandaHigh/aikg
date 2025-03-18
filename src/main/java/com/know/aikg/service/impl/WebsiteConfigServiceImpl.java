package com.know.aikg.service.impl;

import com.know.aikg.entity.WebsiteConfig;
import com.know.aikg.repository.WebsiteConfigRepository;
import com.know.aikg.service.WebsiteConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 网站配置服务实现类
 */
@Service
public class WebsiteConfigServiceImpl implements WebsiteConfigService {
    
    private static final Logger logger = LoggerFactory.getLogger(WebsiteConfigServiceImpl.class);
    
    @Autowired
    private WebsiteConfigRepository websiteConfigRepository;
    
    @Value("${website.max-retry-count:5}")
    private Integer maxRetryCount;
    
    @Override
    public List<WebsiteConfig> findAllEnabled() {
        return websiteConfigRepository.findByEnabledTrue();
    }
    
    @Override
    public List<WebsiteConfig> findEnabledWithRetryAvailable() {
        return websiteConfigRepository.findEnabledWebsitesWithRetryAvailable(maxRetryCount);
    }
    
    @Override
    public List<WebsiteConfig> findFailedWithRetryAvailable() {
        return websiteConfigRepository.findFailedWebsitesWithRetryAvailable(maxRetryCount);
    }
    
    @Override
    public List<WebsiteConfig> findPendingWebsites() {
        return websiteConfigRepository.findPendingWebsites(maxRetryCount);
    }
    
    @Override
    public Optional<WebsiteConfig> findById(Long id) {
        return websiteConfigRepository.findById(id);
    }
    
    @Override
    @Transactional
    public WebsiteConfig save(WebsiteConfig websiteConfig) {
        return websiteConfigRepository.save(websiteConfig);
    }
    
    @Override
    @Transactional
    public void updateToSuccess(Long id) {
        websiteConfigRepository.updateFetchStatus(id, 1, LocalDateTime.now());
        logger.info("网站ID: {} 爬取成功，状态已更新", id);
    }
    
    @Override
    @Transactional
    public void updateToFailed(Long id) {
        websiteConfigRepository.updateFetchStatus(id, 2, LocalDateTime.now());
        logger.info("网站ID: {} 爬取失败，状态已更新", id);
    }
    
    @Override
    @Transactional
    public void resetAllTodayRetryCount() {
        websiteConfigRepository.resetAllTodayRetryCount();
        logger.info("已重置所有网站的今日爬取尝试次数");
    }
} 