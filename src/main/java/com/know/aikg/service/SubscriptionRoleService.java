package com.know.aikg.service;

import com.know.aikg.entity.SubscriptionRole;
import com.know.aikg.repository.SubscriptionRoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 订阅角色服务类
 * 
 * 提供订阅角色相关的业务逻辑处理，包括创建、查询、更新、删除等操作
 * 
 * @Service: 标记该类为Spring服务组件
 */
@Service
public class SubscriptionRoleService {

    /**
     * 日志记录器
     */
    private static final Logger logger = LoggerFactory.getLogger(SubscriptionRoleService.class);
    
    /**
     * 订阅角色数据访问对象
     */
    @Autowired
    private SubscriptionRoleRepository repository;

    /**
     * 事件发布器，用于发布订阅变更事件
     */
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * 创建新订阅
     * 
     * @param subscription 要创建的订阅对象
     * @return 创建成功后的订阅对象（包含生成的ID）
     * 
     * @Transactional: 确保方法在事务中执行
     */
    @Transactional
    public SubscriptionRole create(SubscriptionRole subscription) {
        logger.info("创建新订阅: {}", subscription);
        SubscriptionRole saved = repository.save(subscription);
        logger.debug("订阅创建成功，ID: {}", saved.getId());
        
        // 发布订阅创建事件
        eventPublisher.publishEvent(new SubscriptionChangeEvent(this, saved, SubscriptionChangeType.CREATED));
        
        return saved;
    }

    /**
     * 更新订阅信息
     * 
     * @param subscription 包含更新信息的订阅对象
     * @return 更新后的订阅对象
     * @throws IllegalArgumentException 当订阅ID为空时抛出异常
     * 
     * @Transactional: 确保方法在事务中执行
     */
    @Transactional
    public SubscriptionRole update(SubscriptionRole subscription) {
        if (subscription.getId() == null) {
            logger.error("更新订阅失败: ID不能为空");
            throw new IllegalArgumentException("ID cannot be null for update");
        }
        logger.info("更新订阅，ID: {}", subscription.getId());
        SubscriptionRole updated = repository.save(subscription);
        logger.debug("订阅更新成功，ID: {}", updated.getId());
        
        // 发布订阅更新事件
        eventPublisher.publishEvent(new SubscriptionChangeEvent(this, updated, SubscriptionChangeType.UPDATED));
        
        return updated;
    }

    /**
     * 根据ID查询订阅
     * 
     * @param id 订阅ID
     * @return 包含订阅对象的Optional，如果不存在则为空
     */
    public Optional<SubscriptionRole> findById(String id) {
        logger.debug("通过ID查询订阅: {}", id);
        return repository.findById(id);
    }

    /**
     * 查询所有订阅
     * 
     * @return 所有订阅的列表
     */
    public List<SubscriptionRole> findAll() {
        logger.debug("查询所有订阅");
        List<SubscriptionRole> subscriptions = repository.findAll();
        logger.debug("查询到 {} 个订阅", subscriptions.size());
        return subscriptions;
    }

    /**
     * 查询所有有效（激活状态）的订阅
     * 
     * @return 所有激活状态订阅的列表
     */
    public List<SubscriptionRole> findAllActive() {
        logger.debug("查询所有有效订阅");
        List<SubscriptionRole> subscriptions = repository.findByStatus(true);
        logger.debug("查询到 {} 个有效订阅", subscriptions.size());
        return subscriptions;
    }

    /**
     * 根据邮箱查询订阅
     * 
     * @param email 读者邮箱
     * @return 该邮箱关联的所有订阅列表
     */
    public List<SubscriptionRole> findByEmail(String email) {
        logger.debug("查询邮箱为 {} 的订阅", email);
        List<SubscriptionRole> subscriptions = repository.findByReaderEmail(email);
        logger.debug("查询到 {} 个订阅", subscriptions.size());
        return subscriptions;
    }

    /**
     * 根据领域查询订阅
     * 
     * @param area 订阅领域
     * @return 该领域的所有订阅列表
     */
    public List<SubscriptionRole> findByArea(String area) {
        logger.debug("查询领域为 {} 的订阅", area);
        List<SubscriptionRole> subscriptions = repository.findByArea(area);
        logger.debug("查询到 {} 个订阅", subscriptions.size());
        return subscriptions;
    }

    /**
     * 删除订阅
     * 
     * @param id 要删除的订阅ID
     * 
     * @Transactional: 确保方法在事务中执行
     */
    @Transactional
    public void delete(String id) {
        logger.info("删除订阅，ID: {}", id);
        
        // 获取订阅信息，用于发布事件
        Optional<SubscriptionRole> subscription = repository.findById(id);
        
        repository.deleteById(id);
        logger.debug("订阅删除成功，ID: {}", id);
        
        // 发布订阅删除事件
        subscription.ifPresent(sub -> 
            eventPublisher.publishEvent(new SubscriptionChangeEvent(this, sub, SubscriptionChangeType.DELETED)));
    }

    /**
     * 停用订阅
     * 
     * 将订阅状态设置为false（停用）
     * 
     * @param id 要停用的订阅ID
     * 
     * @Transactional: 确保方法在事务中执行
     */
    @Transactional
    public void deactivate(String id) {
        logger.info("停用订阅，ID: {}", id);
        repository.findById(id).ifPresent(subscription -> {
            subscription.setStatus(false);
            repository.save(subscription);
            logger.debug("订阅已停用，ID: {}", id);
            
            // 发布订阅状态变更事件
            eventPublisher.publishEvent(new SubscriptionChangeEvent(this, subscription, SubscriptionChangeType.DEACTIVATED));
        });
    }
    
    /**
     * 启用订阅
     * 
     * 将订阅状态设置为true（激活）
     * 
     * @param id 要启用的订阅ID
     * 
     * @Transactional: 确保方法在事务中执行
     */
    @Transactional
    public void activate(String id) {
        logger.info("启用订阅，ID: {}", id);
        repository.findById(id).ifPresent(subscription -> {
            subscription.setStatus(true);
            repository.save(subscription);
            logger.debug("订阅已启用，ID: {}", id);
            
            // 发布订阅状态变更事件
            eventPublisher.publishEvent(new SubscriptionChangeEvent(this, subscription, SubscriptionChangeType.ACTIVATED));
        });
    }
    
    /**
     * 更新订阅的定时任务表达式
     * 
     * @param id 订阅ID
     * @param cronExpression 新的cron表达式
     * @return 更新后的订阅对象
     * 
     * @Transactional: 确保方法在事务中执行
     */
    @Transactional
    public SubscriptionRole updateScheduleCron(String id, String cronExpression) {
        logger.info("更新订阅定时任务表达式，ID: {}, 表达式: {}", id, cronExpression);
        
        Optional<SubscriptionRole> optionalSubscription = repository.findById(id);
        if (optionalSubscription.isEmpty()) {
            logger.error("更新定时任务表达式失败: 未找到ID为{}的订阅", id);
            throw new IllegalArgumentException("Subscription not found with ID: " + id);
        }
        
        SubscriptionRole subscription = optionalSubscription.get();
        subscription.setScheduleCron(cronExpression);
        SubscriptionRole updated = repository.save(subscription);
        logger.debug("订阅定时任务表达式更新成功，ID: {}", id);
        
        // 发布订阅更新事件
        eventPublisher.publishEvent(new SubscriptionChangeEvent(this, updated, SubscriptionChangeType.SCHEDULE_UPDATED));
        
        return updated;
    }
    
    /**
     * 订阅变更类型枚举
     */
    public enum SubscriptionChangeType {
        CREATED,      // 创建
        UPDATED,      // 更新
        DELETED,      // 删除
        ACTIVATED,    // 激活
        DEACTIVATED,  // 停用
        SCHEDULE_UPDATED // 定时任务更新
    }
    
    /**
     * 订阅变更事件类
     */
    public static class SubscriptionChangeEvent {
        private final Object source;
        private final SubscriptionRole subscription;
        private final SubscriptionChangeType type;
        
        public SubscriptionChangeEvent(Object source, SubscriptionRole subscription, SubscriptionChangeType type) {
            this.source = source;
            this.subscription = subscription;
            this.type = type;
        }
        
        public Object getSource() {
            return source;
        }
        
        public SubscriptionRole getSubscription() {
            return subscription;
        }
        
        public SubscriptionChangeType getType() {
            return type;
        }
    }
} 