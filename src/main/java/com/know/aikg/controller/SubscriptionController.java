package com.know.aikg.controller;

import com.know.aikg.entity.SubscriptionRole;
import com.know.aikg.service.GenerateService;
import com.know.aikg.service.SubscriptionRoleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订阅管理控制器
 * 
 * 提供订阅相关的RESTful API接口，包括订阅的创建、查询、更新、删除等功能
 * 
 * @RestController: 标记该类为REST控制器，所有方法返回的数据会自动序列化为JSON
 * @RequestMapping: 指定该控制器的基础URL路径
 * @CrossOrigin: 允许跨域请求
 * @Validated: 启用方法级别的参数验证
 */
@RestController
@RequestMapping("/api/subscriptions")
@CrossOrigin
@Validated
public class SubscriptionController {

    /**
     * 日志记录器
     */
    private static final Logger logger = LoggerFactory.getLogger(SubscriptionController.class);

    /**
     * 订阅角色服务，用于处理订阅相关的业务逻辑
     */
    @Autowired
    private SubscriptionRoleService subscriptionRoleService;
    
    /**
     * 生成服务，用于生成内容并发送邮件
     */
    @Autowired
    private GenerateService generateService;

    /**
     * 获取订阅列表
     * 
     * @param readerEmail 可选的读者邮箱参数，用于筛选特定读者的订阅
     * @return 订阅列表
     * 
     * HTTP方法: GET
     * 路径: /api/subscriptions
     */
    @GetMapping
    public List<SubscriptionRole> getAllSubscriptions(@RequestParam(required = false) @Email(message = "邮箱格式不正确") String readerEmail) {
        // 如果提供了邮箱参数，根据邮箱查询
        if (readerEmail != null && !readerEmail.trim().isEmpty()) {
            logger.info("查询邮箱为 {} 的订阅列表", readerEmail);
            return subscriptionRoleService.findByEmail(readerEmail);
        }
        // 否则返回空订阅列表
        return new ArrayList<SubscriptionRole>();
    }

    /**
     * 创建新订阅
     * 
     * @param subscription 订阅信息对象
     * @return 创建成功的订阅对象（包含生成的ID）
     * 
     * HTTP方法: POST
     * 路径: /api/subscriptions
     */
    @PostMapping
    public SubscriptionRole createSubscription(@Valid @RequestBody SubscriptionRole subscription) {
        logger.info("创建新订阅: {}", subscription);
        SubscriptionRole result = subscriptionRoleService.create(subscription);
        logger.info("订阅创建成功，ID: {}", result.getId());
        return result;
    }

    /**
     * 更新订阅信息
     * 
     * @param id 订阅ID
     * @param subscription 更新后的订阅信息
     * @return 更新后的订阅对象
     * 
     * HTTP方法: PUT
     * 路径: /api/subscriptions/{id}
     */
    @PutMapping("/{id}")
    public SubscriptionRole updateSubscription(
            @PathVariable @NotNull(message = "ID不能为空") String id, 
            @Valid @RequestBody SubscriptionRole subscription) {
        logger.info("更新订阅，ID: {}", id);
        subscription.setId(id);
        SubscriptionRole result = subscriptionRoleService.update(subscription);
        logger.info("订阅更新成功，ID: {}", result.getId());
        return result;
    }

    /**
     * 删除订阅
     * 
     * @param id 要删除的订阅ID
     * @return 操作结果消息
     * 
     * HTTP方法: DELETE
     * 路径: /api/subscriptions/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteSubscription(@PathVariable @NotNull(message = "ID不能为空") String id) {
        logger.info("删除订阅，ID: {}", id);
        subscriptionRoleService.delete(id);
        logger.info("订阅删除成功，ID: {}", id);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "订阅已成功删除");
        return ResponseEntity.ok(response);
    }

    /**
     * 根据ID获取订阅详情
     * 
     * @param id 订阅ID
     * @return 订阅详情对象
     * @throws RuntimeException 当订阅不存在时抛出异常
     * 
     * HTTP方法: GET
     * 路径: /api/subscriptions/{id}
     */
    @GetMapping("/{id}")
    public SubscriptionRole getSubscriptionById(@PathVariable @NotNull(message = "ID不能为空") String id) {
        logger.info("查询订阅详情，ID: {}", id);
        try {
            SubscriptionRole result = subscriptionRoleService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Subscription not found with id: " + id));
            logger.info("查询订阅成功，ID: {}", id);
            return result;
        } catch (Exception e) {
            logger.error("查询订阅失败，ID: {}，错误信息: {}", id, e.getMessage());
            throw e;
        }
    }
    
    /**
     * 停用订阅
     * 
     * @param id 订阅ID
     * @return 更新后的订阅对象
     * @throws RuntimeException 当订阅不存在时抛出异常
     * 
     * HTTP方法: PUT
     * 路径: /api/subscriptions/{id}/deactivate
     */
    @PutMapping("/{id}/deactivate")
    public SubscriptionRole deactivateSubscription(@PathVariable @NotNull(message = "ID不能为空") String id) {
        logger.info("停用订阅，ID: {}", id);
        subscriptionRoleService.deactivate(id);
        try {
            SubscriptionRole result = subscriptionRoleService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Subscription not found with id: " + id));
            logger.info("订阅停用成功，ID: {}", id);
            return result;
        } catch (Exception e) {
            logger.error("订阅停用失败，ID: {}，错误信息: {}", id, e.getMessage());
            throw e;
        }
    }
    
    /**
     * 启用订阅
     * 
     * @param id 订阅ID
     * @return 更新后的订阅对象
     * @throws RuntimeException 当订阅不存在时抛出异常
     * 
     * HTTP方法: PUT
     * 路径: /api/subscriptions/{id}/activate
     */
    @PutMapping("/{id}/activate")
    public SubscriptionRole activateSubscription(@PathVariable @NotNull(message = "ID不能为空") String id) {
        logger.info("启用订阅，ID: {}", id);
        subscriptionRoleService.activate(id);
        try {
            SubscriptionRole result = subscriptionRoleService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Subscription not found with id: " + id));
            logger.info("订阅启用成功，ID: {}", id);
            return result;
        } catch (Exception e) {
            logger.error("订阅启用失败，ID: {}，错误信息: {}", id, e.getMessage());
            throw e;
        }
    }
    
    /**
     * 测试邮件发送
     * 
     * 根据订阅ID生成内容并发送测试邮件
     * 
     * @param id 订阅ID
     * @return 操作结果消息
     * 
     * HTTP方法: GET
     * 路径: /api/subscriptions/{id}/test/send-email
     */
    @GetMapping("/{id}/test/send-email")
    public ResponseEntity<Map<String, String>> generateAndSendEmail(@PathVariable @NotNull(message = "ID不能为空") String id) {
        logger.info("尝试发送测试邮件，订阅ID: {}", id);
        return subscriptionRoleService.findById(id)
                .map(subscription -> {
                    logger.info("开始生成并发送测试邮件，订阅ID: {}，接收者: {}", id, subscription.getReaderEmail());
                    generateService.generateAndSendEmail(subscription.getArea(), subscription.getReader(), subscription.getReaderEmail());
                    Map<String, String> response = new HashMap<>();
                    response.put("message", "邮件已成功发送至 " + subscription.getReaderEmail());
                    
                    logger.info("测试邮件发送成功，订阅ID: {}，接收者: {}", id, subscription.getReaderEmail());
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    logger.error("发送测试邮件失败，订阅ID: {}，错误原因: 订阅不存在", id);
                    Map<String, String> error = new HashMap<>();
                    error.put("message", "订阅不存在");
                    return ResponseEntity.badRequest().body(error);
                });
    }
    
    /**
     * 测试文章生成并发送（无需订阅ID）
     * 
     * 根据提供的领域、读者和邮箱信息生成内容并发送测试邮件
     * 
     * @param testData 包含领域、读者和邮箱信息的测试数据
     * @return 操作结果消息
     * 
     * HTTP方法: POST
     * 路径: /api/subscriptions/test/generate-and-send
     */
    @PostMapping("/test/generate-and-send")
    public ResponseEntity<Map<String, String>> testGenerateAndSend(@Valid @RequestBody TestSubscriptionRequest testData) {
        logger.info("尝试发送测试邮件，领域: {}，读者: {}，邮箱: {}", 
                testData.getArea(), testData.getReader(), testData.getReaderEmail());
        
        try {
            // 异步执行，不阻塞请求
            new Thread(() -> {
                try {
                    generateService.generateAndSendEmail(testData.getArea(), testData.getReader(), testData.getReaderEmail());
                    logger.info("测试邮件发送成功，接收者: {}", testData.getReaderEmail());
                } catch (Exception e) {
                    logger.error("测试邮件发送失败，接收者: {}，错误: {}", testData.getReaderEmail(), e.getMessage(), e);
                }
            }).start();
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "测试请求已提交，文章生成完成后将发送至 " + testData.getReaderEmail());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("提交测试请求失败，错误: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("message", "提交测试请求失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * 测试订阅请求类
     * 
     * 用于接收测试文章生成的请求参数
     */
    public static class TestSubscriptionRequest {
        @NotBlank(message = "领域不能为空")
        private String area;
        
        @NotBlank(message = "读者不能为空")
        private String reader;
        
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        private String readerEmail;
        
        // Getters and Setters
        public String getArea() {
            return area;
        }
        
        public void setArea(String area) {
            this.area = area;
        }
        
        public String getReader() {
            return reader;
        }
        
        public void setReader(String reader) {
            this.reader = reader;
        }
        
        public String getReaderEmail() {
            return readerEmail;
        }
        
        public void setReaderEmail(String readerEmail) {
            this.readerEmail = readerEmail;
        }
    }

    /**
     * 更新订阅的定时任务表达式
     * 
     * @param id 订阅ID
     * @param request 包含cron表达式的请求对象
     * @return 更新后的订阅对象
     * 
     * HTTP方法: PUT
     * 路径: /api/subscriptions/{id}/schedule
     */
    @PutMapping("/{id}/schedule")
    public SubscriptionRole updateSubscriptionSchedule(
            @PathVariable @NotNull(message = "ID不能为空") String id, 
            @Valid @RequestBody ScheduleUpdateRequest request) {
        logger.info("更新订阅定时任务表达式，ID: {}, 表达式: {}", id, request.getScheduleCron());
        
        try {
            SubscriptionRole result = subscriptionRoleService.updateScheduleCron(id, request.getScheduleCron());
            logger.info("订阅定时任务表达式更新成功，ID: {}", id);
            return result;
        } catch (Exception e) {
            logger.error("更新订阅定时任务表达式失败，ID: {}，错误信息: {}", id, e.getMessage());
            throw e;
        }
    }

    /**
     * 定时任务表达式更新请求类
     */
    public static class ScheduleUpdateRequest {
        
        @NotBlank(message = "定时任务表达式不能为空")
        private String scheduleCron;
        
        public String getScheduleCron() {
            return scheduleCron;
        }
        
        public void setScheduleCron(String scheduleCron) {
            this.scheduleCron = scheduleCron;
        }
    }
} 