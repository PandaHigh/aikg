package com.know.aikg.controller;

import com.know.aikg.entity.WebsiteConfig;
import com.know.aikg.service.WebsiteConfigService;
import com.know.aikg.service.ArticleScraperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 网站配置控制器
 */
@RestController
@RequestMapping("/api/websites")
public class WebsiteConfigController {
    
    private static final Logger logger = LoggerFactory.getLogger(WebsiteConfigController.class);
    
    @Autowired
    private WebsiteConfigService websiteConfigService;
    
    @Autowired
    private ArticleScraperService articleScraperService;
    
    /**
     * 获取所有网站配置
     */
    @GetMapping
    public ResponseEntity<List<WebsiteConfig>> getAllWebsites() {
        List<WebsiteConfig> websites = websiteConfigService.findAllEnabled();
        return ResponseEntity.ok(websites);
    }
    
    /**
     * 获取特定网站配置
     */
    @GetMapping("/{id}")
    public ResponseEntity<WebsiteConfig> getWebsite(@PathVariable Long id) {
        Optional<WebsiteConfig> website = websiteConfigService.findById(id);
        return website.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 创建网站配置
     */
    @PostMapping
    public ResponseEntity<WebsiteConfig> createWebsite(@RequestBody WebsiteConfig website) {
        // 设置默认值
        website.setTodayRetryCount(0);
        website.setLastFetchStatus(0);
        
        WebsiteConfig savedWebsite = websiteConfigService.save(website);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedWebsite);
    }
    
    /**
     * 更新网站配置
     */
    @PutMapping("/{id}")
    public ResponseEntity<WebsiteConfig> updateWebsite(@PathVariable Long id, @RequestBody WebsiteConfig website) {
        Optional<WebsiteConfig> existingWebsite = websiteConfigService.findById(id);
        
        if (existingWebsite.isPresent()) {
            WebsiteConfig updatedWebsite = existingWebsite.get();
            updatedWebsite.setName(website.getName());
            updatedWebsite.setUrl(website.getUrl());
            updatedWebsite.setBaseDomain(website.getBaseDomain());
            updatedWebsite.setEnabled(website.getEnabled());
            
            return ResponseEntity.ok(websiteConfigService.save(updatedWebsite));
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 启用或禁用网站
     */
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<Map<String, Object>> toggleWebsite(@PathVariable Long id) {
        Optional<WebsiteConfig> existingWebsite = websiteConfigService.findById(id);
        
        if (existingWebsite.isPresent()) {
            WebsiteConfig website = existingWebsite.get();
            website.setEnabled(!website.getEnabled());
            websiteConfigService.save(website);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", website.getId());
            response.put("enabled", website.getEnabled());
            
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 手动触发网站爬取
     */
    @PostMapping("/{id}/scrape")
    public ResponseEntity<Map<String, Object>> scrapeWebsite(@PathVariable Long id) {
        Optional<WebsiteConfig> existingWebsite = websiteConfigService.findById(id);
        
        if (existingWebsite.isPresent()) {
            WebsiteConfig website = existingWebsite.get();
            
            // 异步执行爬取任务
            new Thread(() -> {
                articleScraperService.executeArticleScraping(website);
            }).start();
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", website.getId());
            response.put("name", website.getName());
            response.put("message", "网站爬取任务已启动");
            
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 重置所有网站的今日爬取尝试次数
     */
    @PostMapping("/reset-retry-count")
    public ResponseEntity<Map<String, Object>> resetRetryCount() {
        websiteConfigService.resetAllTodayRetryCount();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "已重置所有网站的今日爬取尝试次数");
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }
} 