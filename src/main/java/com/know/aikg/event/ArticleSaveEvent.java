package com.know.aikg.event;

import org.springframework.context.ApplicationEvent;
import java.util.List;
import java.util.Map;

/**
 * 文章保存事件
 */
public class ArticleSaveEvent extends ApplicationEvent {
    
    private final List<Map<String, String>> articles;
    private final Long subscriptionRoleId;
    
    /**
     * 创建一个新的文章保存事件
     * 
     * @param source 事件源
     * @param articles 要保存的文章列表
     * @param subscriptionRoleId 订阅角色ID
     */
    public ArticleSaveEvent(Object source, List<Map<String, String>> articles, Long subscriptionRoleId) {
        super(source);
        this.articles = articles;
        this.subscriptionRoleId = subscriptionRoleId;
    }
    
    /**
     * 获取要保存的文章列表
     * 
     * @return 文章列表
     */
    public List<Map<String, String>> getArticles() {
        return articles;
    }
    
    /**
     * 获取订阅角色ID
     * 
     * @return 订阅角色ID
     */
    public Long getSubscriptionRoleId() {
        return subscriptionRoleId;
    }
} 