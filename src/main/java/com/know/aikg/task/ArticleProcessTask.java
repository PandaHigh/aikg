package com.know.aikg.task;

import com.know.aikg.entity.Article;
import com.know.aikg.service.ArticleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 文章处理定时任务
 */
@Component
public class ArticleProcessTask {
    
    private static final Logger logger = LoggerFactory.getLogger(ArticleProcessTask.class);
    
    @Autowired
    private ArticleService articleService;
    
    @Value("${article.batch-size:10}")
    private Integer batchSize;
    
    @Value("${article.timeout-minutes:30}")
    private Integer timeoutMinutes;
    
    /**
     * 定期处理待处理的文章
     * 每5分钟执行一次
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void processPendingArticles() {
        logger.info("开始处理待处理的文章...");
        
        try {
            // 查找待处理的文章
            List<Article> pendingArticles = articleService.findPendingArticles(batchSize);
            
            if (pendingArticles.isEmpty()) {
                logger.info("没有待处理的文章");
                return;
            }
            
            logger.info("找到 {} 篇待处理的文章", pendingArticles.size());
            
            // 异步处理每篇文章
            for (Article article : pendingArticles) {
                articleService.processArticleAsync(article.getId());
            }
            
        } catch (Exception e) {
            logger.error("处理待处理文章时发生错误", e);
        }
    }
    
    /**
     * 定期检查处理中但可能已超时的文章
     * 每30分钟执行一次
     */
    @Scheduled(cron = "0 */30 * * * ?")
    public void checkTimeoutArticles() {
        logger.info("开始检查处理中但可能已超时的文章...");
        
        try {
            // 查找处理中但可能已超时的文章
            List<Article> timeoutArticles = articleService.findTimeoutArticles(timeoutMinutes);
            
            if (timeoutArticles.isEmpty()) {
                logger.info("没有处理中但可能已超时的文章");
                return;
            }
            
            logger.info("找到 {} 篇处理中但可能已超时的文章", timeoutArticles.size());
            
            // 重新处理每篇超时的文章
            for (Article article : timeoutArticles) {
                logger.info("重新处理超时文章: id={}, title={}", article.getId(), article.getTitle());
                articleService.processArticleAsync(article.getId());
            }
            
        } catch (Exception e) {
            logger.error("检查超时文章时发生错误", e);
        }
    }
} 