package com.know.aikg.event;

import org.springframework.context.ApplicationEvent;

/**
 * 文章爬取事件
 */
public class ArticleScrapingEvent extends ApplicationEvent {
    
    private final boolean success;
    private final int articleCount;
    
    /**
     * 创建一个新的文章爬取事件
     * 
     * @param source 事件源
     * @param success 爬取是否成功
     * @param articleCount 爬取的文章数量
     */
    public ArticleScrapingEvent(Object source, boolean success, int articleCount) {
        super(source);
        this.success = success;
        this.articleCount = articleCount;
    }
    
    /**
     * 获取爬取是否成功
     * 
     * @return 爬取是否成功
     */
    public boolean isSuccess() {
        return success;
    }
    
    /**
     * 获取爬取的文章数量
     * 
     * @return 爬取的文章数量
     */
    public int getArticleCount() {
        return articleCount;
    }
} 