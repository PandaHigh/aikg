package com.know.aikg.service.impl;

import com.know.aikg.entity.Article;
import com.know.aikg.event.ArticleSaveEvent;
import com.know.aikg.repository.ArticleRepository;
import com.know.aikg.service.ArticleService;
import com.know.aikg.service.ArticleScraperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * 文章服务实现类
 */
@Service
public class ArticleServiceImpl implements ArticleService {
    
    private static final Logger logger = LoggerFactory.getLogger(ArticleServiceImpl.class);
    
    @Autowired
    private ArticleRepository articleRepository;
    
    @Autowired
    private ArticleScraperService articleScraperService;
    
    @Value("${article.max-retry-count:3}")
    private Integer maxRetryCount;
    
    @Value("${article.timeout-minutes:30}")
    private Integer timeoutMinutes;
    
    /**
     * 监听文章保存事件
     * @param event 文章保存事件
     */
    @EventListener
    @Transactional
    public void handleArticleSaveEvent(ArticleSaveEvent event) {
        List<Map<String, String>> articles = event.getArticles();
        Long subscriptionRoleId = event.getSubscriptionRoleId();
        
        int savedCount = saveArticleList(articles, subscriptionRoleId);
        logger.info("成功保存 {} 篇文章到数据库", savedCount);
    }
    
    @Override
    @Transactional
    public int saveArticleList(List<Map<String, String>> articles, Long subscriptionRoleId) {
        if (articles == null || articles.isEmpty()) {
            return 0;
        }
        
        int savedCount = 0;
        List<Article> articlesToSave = new ArrayList<>();
        
        // 获取文章来源URL（爬取文章列表的URL）
        String sourceUrl = null;
        if (!articles.isEmpty() && articles.get(0).containsKey("source_url")) {
            sourceUrl = articles.get(0).get("source_url");
        }
        
        for (Map<String, String> articleMap : articles) {
            String title = articleMap.get("title");
            String url = articleMap.get("url");
            
            if (title == null || url == null || title.isEmpty() || url.isEmpty()) {
                logger.warn("跳过无效文章: title={}, url={}", title, url);
                continue;
            }
            
            // 检查URL是否已存在
            Optional<Article> existingArticle = articleRepository.findByUrl(url);
            if (existingArticle.isPresent()) {
                logger.info("文章URL已存在，跳过: {}", url);
                continue;
            }
            
            // 创建新文章对象
            Article article = Article.builder()
                    .title(title)
                    .url(url)
                    .sourceUrl(sourceUrl) // 设置文章来源URL
                    .status(0) // 待处理状态
                    .processCount(0)
                    .subscriptionRoleId(subscriptionRoleId)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();
            
            articlesToSave.add(article);
            savedCount++;
        }
        
        if (!articlesToSave.isEmpty()) {
            articleRepository.saveAll(articlesToSave);
            logger.info("成功保存 {} 篇文章", savedCount);
            
            // 异步处理每篇文章
            for (Article article : articlesToSave) {
                CompletableFuture.runAsync(() -> processArticleAsync(article.getId()));
            }
        }
        
        return savedCount;
    }
    
    @Override
    public Optional<Article> findById(Long id) {
        return articleRepository.findById(id);
    }
    
    @Override
    public Optional<Article> findByUrl(String url) {
        return articleRepository.findByUrl(url);
    }
    
    @Override
    public Page<Article> findAll(Pageable pageable) {
        return articleRepository.findAll(pageable);
    }
    
    @Override
    public Page<Article> findByStatus(Integer status, Pageable pageable) {
        return articleRepository.findByStatus(status, pageable);
    }
    
    @Override
    public List<Article> findPendingArticles(int limit) {
        List<Article> pendingArticles = articleRepository.findPendingArticles(maxRetryCount);
        if (pendingArticles.size() > limit) {
            return pendingArticles.subList(0, limit);
        }
        return pendingArticles;
    }
    
    @Override
    public List<Article> findTimeoutArticles(int timeoutMinutes) {
        return articleRepository.findTimeoutArticles(timeoutMinutes, maxRetryCount);
    }
    
    @Override
    @Transactional
    public Article updateToProcessing(Article article) {
        article.setStatus(1); // 处理中状态
        article.setProcessCount(article.getProcessCount() + 1);
        article.setUpdateTime(LocalDateTime.now());
        return articleRepository.save(article);
    }
    
    @Override
    @Transactional
    public Article updateContent(Article article, String summary) {
        article.setSummary(summary);
        article.setUpdateTime(LocalDateTime.now());
        return articleRepository.save(article);
    }
    
    @Override
    @Transactional
    public Article updateToCompleted(Article article) {
        article.setStatus(2); // 处理完成状态
        article.setUpdateTime(LocalDateTime.now());
        return articleRepository.save(article);
    }
    
    @Override
    @Transactional
    public Article updateToFailed(Article article, String errorMessage) {
        article.setStatus(3); // 处理失败状态
        article.setErrorMessage(errorMessage);
        article.setUpdateTime(LocalDateTime.now());
        return articleRepository.save(article);
    }
    
    @Override
    @Async
    public void processArticleAsync(Long articleId) {
        logger.info("开始异步处理文章: id={}", articleId);
        
        Optional<Article> optionalArticle = articleRepository.findById(articleId);
        if (!optionalArticle.isPresent()) {
            logger.error("找不到文章: id={}", articleId);
            return;
        }
        
        Article article = optionalArticle.get();
        
        
        // 更新文章状态为处理中
        try {
            article = updateToProcessing(article);
            logger.info("文章状态已更新为处理中: id={}", articleId);
            
            // 获取文章内容
            String url = article.getUrl();
            String title = article.getTitle();
            
            // 调用爬虫服务获取文章内容
            Map<String, String> contentMap = articleScraperService.fetchArticleContent(url);
            
            if (contentMap == null || contentMap.isEmpty()) {
                logger.error("获取文章内容失败: id={}, url={}", articleId, url);
                updateToFailed(article, "获取文章内容失败");
                return;
            }
            
            String textContent = contentMap.get("text_content");
            
            if (textContent == null || textContent.isEmpty()) {
                logger.error("文章内容为空: id={}, url={}", articleId, url);
                updateToFailed(article, "文章内容为空");
                return;
            }
            
            // 使用爬取的内容生成摘要，但不存储内容
            String summary = articleScraperService.generateSummaryFromContent(textContent, title);
            
            if (summary == null || summary.isEmpty()) {
                logger.error("生成摘要失败: id={}, url={}", articleId, url);
                updateToFailed(article, "生成摘要失败");
                return;
            }
            
            // 只更新文章摘要，不存储内容
            article = updateContent(article, summary);
            logger.info("文章摘要已更新: id={}", articleId);
            
            // 更新文章状态为处理完成
            updateToCompleted(article);
            logger.info("文章处理完成: id={}", articleId);
            
        } catch (Exception e) {
            logger.error("处理文章时发生错误: id={}, error={}", articleId, e.getMessage(), e);
            updateToFailed(article, "处理文章时发生错误: " + e.getMessage());
        }
    }
} 