package com.know.aikg.service;

import com.know.aikg.entity.SubscriptionRole;
import com.know.aikg.service.SubscriptionRoleService.SubscriptionChangeEvent;
import com.know.aikg.service.SubscriptionRoleService.SubscriptionChangeType;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import jakarta.annotation.PostConstruct;
import java.util.HashMap;

/**
 * 内容生成服务
 * 
 * 负责生成AI内容并通过邮件发送给订阅用户
 * 包含定时任务功能，可以按计划自动生成和发送内容
 * 
 * @Service: 标记该类为Spring服务组件
 */
@Service
public class GenerateService {

    /**
     * 日志记录器
     */
    private static final Logger logger = LoggerFactory.getLogger(GenerateService.class);
    
    /**
     * 日期时间格式化器，用于日志和报告中的时间格式化
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 邮件服务，用于发送生成的内容
     */
    @Autowired
    private EmailService emailService;

    /**
     * AI服务，用于调用大语言模型生成内容
     */
    @Autowired
    private AIService aiService;

    /**
     * 订阅角色服务，用于获取活跃的订阅信息
     */
    @Autowired
    private SubscriptionRoleService subscriptionRoleService;

    /**
     * 任务调度器，用于管理动态定时任务
     */
    @Autowired
    private TaskScheduler taskScheduler;

    /**
     * 管理员邮箱列表，用于发送任务执行报告
     * 从配置文件中注入
     */
    @Value("${email.admin.recipients}")
    private String[] emailAdminRecipients;
    
    
    /**
     * 每日邮件发送定时任务的cron表达式
     * 从配置文件中注入
     */
    @Value("${aikg.schedule.daily-email}")
    private String dailyEmailCron;


    // 存储每个订阅的定时任务
    private Map<String, ScheduledFuture<?>> scheduledTasks = new HashMap<>();

    /**
     * 应用启动时初始化所有订阅的定时任务
     */
    @PostConstruct
    public void initScheduledTasks() {
        logger.info("初始化订阅定时任务...");
        // 获取所有活跃的订阅
        List<SubscriptionRole> activeSubscriptions = subscriptionRoleService.findAllActive();
        
        // 为每个订阅创建定时任务
        for (SubscriptionRole subscription : activeSubscriptions) {
            scheduleSubscription(subscription);
        }
        
        logger.info("成功初始化 {} 个订阅定时任务", activeSubscriptions.size());
    }
    
    /**
     * 为指定订阅创建或更新定时任务
     * 
     * @param subscription 订阅对象
     */
    public void scheduleSubscription(SubscriptionRole subscription) {
        // 如果订阅没有激活，则不创建定时任务
        if (!subscription.getStatus()) {
            logger.info("订阅 ID: {} 未激活，跳过定时任务创建", subscription.getId());
            return;
        }
        
        // 获取订阅的cron表达式，如果为空则使用默认值
        String cronExpression = subscription.getScheduleCron();
        if (cronExpression == null || cronExpression.trim().isEmpty()) {
            cronExpression = dailyEmailCron;
        }
        
        // 取消已存在的定时任务
        cancelScheduledTask(subscription.getId());
        
        // 创建新的定时任务
        ScheduledFuture<?> scheduledTask = taskScheduler.schedule(
            () -> generateAndSendEmailForSubscription(subscription),
            new CronTrigger(cronExpression)
        );
        
        // 保存定时任务引用
        scheduledTasks.put(subscription.getId(), scheduledTask);
        
        logger.info("已为订阅 ID: {} 创建定时任务，cron表达式: {}", subscription.getId(), cronExpression);
    }
    
    /**
     * 取消指定订阅的定时任务
     * 
     * @param subscriptionId 订阅ID
     */
    public void cancelScheduledTask(String subscriptionId) {
        ScheduledFuture<?> scheduledTask = scheduledTasks.get(subscriptionId);
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
            scheduledTasks.remove(subscriptionId);
            logger.info("已取消订阅 ID: {} 的定时任务", subscriptionId);
        }
    }
    
    /**
     * 为指定的订阅生成并发送邮件
     * 
     * @param subscription 订阅对象
     */
    private void generateAndSendEmailForSubscription(SubscriptionRole subscription) {
        String taskId = "task-" + System.currentTimeMillis() + "-" + subscription.getId();
        LocalDateTime startTime = LocalDateTime.now();
        
        logger.info("[任务开始] ID: {}, 时间: {}, 订阅ID: {}, 领域: {}, 读者: {}", 
                taskId, startTime.format(DATE_FORMATTER), subscription.getId(), 
                subscription.getArea(), subscription.getReader());
        
        try {
            // 为当前订阅生成内容并发送邮件
            generateAndSendEmail(subscription.getArea(), subscription.getReader(), subscription.getReaderEmail());
            
            // 记录成功
            logger.info("[处理成功] ID: {}, 订阅ID: {}, 领域: {}, 读者: {}", 
                    taskId, subscription.getId(), subscription.getArea(), subscription.getReader());
        } catch (Exception e) {
            // 记录失败
            logger.error("[处理失败] ID: {}, 订阅ID: {}, 错误: {}", 
                    taskId, subscription.getId(), e.getMessage(), e);
            
            // 发送失败报告给管理员
            sendFailureReportToAdmin(taskId, 0, 1, 1);
        }
        
        // 计算任务耗时
        LocalDateTime endTime = LocalDateTime.now();
        long durationSeconds = java.time.Duration.between(startTime, endTime).getSeconds();
        
        logger.info("[任务完成] ID: {}, 耗时: {}秒", taskId, durationSeconds);
    }

    /**
     * 全局定时任务，用于检查和更新所有订阅的定时任务
     * 每天凌晨1点执行一次，确保所有定时任务都是最新的
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void refreshAllScheduledTasks() {
        logger.info("开始刷新所有订阅定时任务...");
        
        // 获取所有活跃的订阅
        List<SubscriptionRole> activeSubscriptions = subscriptionRoleService.findAllActive();
        
        // 更新所有定时任务
        for (SubscriptionRole subscription : activeSubscriptions) {
            scheduleSubscription(subscription);
        }
        
        logger.info("成功刷新 {} 个订阅定时任务", activeSubscriptions.size());
    }

    /**
     * 发送失败报告给管理员
     * 
     * 当有订阅处理失败时，向管理员发送汇总报告
     * 
     * @param taskId 任务ID
     * @param success 成功处理的订阅数量
     * @param failure 处理失败的订阅数量
     * @param total 总订阅数量
     */
    private void sendFailureReportToAdmin(String taskId, int success, int failure, int total) {
        try {
            String subject = "AIKG任务执行报告 - 部分失败";
            String content = String.format(
                "任务ID: %s\n执行时间: %s\n成功数: %d\n失败数: %d\n总数: %d\n\n请检查系统日志获取详细信息。",
                taskId, LocalDateTime.now().format(DATE_FORMATTER), success, failure, total
            );
            
            logger.info("[发送报告] 向管理员发送执行报告");
            emailService.sendBatchEmail(emailAdminRecipients, subject, content);
        } catch (Exception e) {
            logger.error("[发送报告失败] 无法向管理员发送报告: {}", e.getMessage(), e);
        }
    }

    /**
     * 生成内容并发送邮件
     * 
     * 核心方法，为指定领域和读者生成内容并发送到指定邮箱
     * 
     * @param area 领域，内容的主题领域
     * @param reader 读者，内容的目标受众
     * @param readerEmail 读者邮箱，接收内容的邮箱地址
     */
    public void generateAndSendEmail(String area, String reader, String readerEmail) {
        LocalDateTime startTime = LocalDateTime.now();
        String operationId = "op-" + System.currentTimeMillis();
        
        logger.info("[邮件生成开始] ID: {}, 领域: {}, 读者: {}, 邮箱: {}", 
                operationId, area, reader, readerEmail);
        
        try {
            // 第一步：生成60个文章标题
            long titleStartTime = System.currentTimeMillis();
            List<String> titles = generateArticleTitles(area, reader);
            long titleEndTime = System.currentTimeMillis();
            logger.debug("[标题生成] ID: {}, 耗时: {}ms, 生成标题数: {}", 
                    operationId, (titleEndTime - titleStartTime), titles.size());
            
            // 第二步：随机选择一个标题
            String selectedTitle = selectRandomTitle(titles);
            logger.info("[选择标题] ID: {}, 选中标题: {}", operationId, selectedTitle);
            
            // 第三步：根据选中的标题生成文章内容
            long contentStartTime = System.currentTimeMillis();
            String emailContent = generateContentPrompt(area, reader, selectedTitle);
            emailContent = convertMarkdownToPlainText(emailContent);
            long contentEndTime = System.currentTimeMillis();
            logger.debug("[内容生成] ID: {}, 耗时: {}ms, 长度: {} 字符", 
                    operationId, (contentEndTime - contentStartTime), emailContent.length());
            
            // 设置邮件主题
            String subject = "AIKG-" + selectedTitle;
            
            // 记录邮件发送时间
            long emailStartTime = System.currentTimeMillis();
            emailService.sendEmail(readerEmail, subject, emailContent);
            long emailEndTime = System.currentTimeMillis();
            
            // 计算总耗时
            LocalDateTime endTime = LocalDateTime.now();
            long totalTimeMillis = java.time.Duration.between(startTime, endTime).toMillis();
            
            // 记录完成情况
            logger.info("[邮件发送完成] ID: {}, 总耗时: {}ms, 邮件发送耗时: {}ms, 主题: {}, 接收者: {}", 
                    operationId, totalTimeMillis, (emailEndTime - emailStartTime), subject, readerEmail);
            
            // 记录详细的性能指标
            logger.info("[性能指标] ID: {}, 标题生成: {}ms, 内容生成: {}ms, 邮件发送: {}ms, 总计: {}ms",
                    operationId, 
                    (titleEndTime - titleStartTime),
                    (contentEndTime - contentStartTime),
                    (emailEndTime - emailStartTime),
                    totalTimeMillis);
        } catch (Exception e) {
            // 记录处理失败的情况
            logger.error("[邮件生成失败] ID: {}, 领域: {}, 读者: {}, 错误: {}", 
                    operationId, area, reader, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 生成文章标题列表
     * 
     * 根据领域和读者信息，使用AI生成60个适合的文章标题
     * 
     * @param area 领域
     * @param reader 读者
     * @return 生成的文章标题列表
     */
    private List<String> generateArticleTitles(String area, String reader) {
        String systemPrompt = 
            "你是一个专业的内容创作者，擅长为特定领域和目标读者创作吸引人的标题。\n" +
            "请以纯文本格式回复，不要使用Markdown或其他格式。\n" +
            "在生成标题前，请先深入思考该领域的核心概念和目标读者的兴趣点。\n" +
            "你需要生成60个主题完全不同的标题，每个标题应该：\n" +
            "1. 吸引人且围绕一个具体知识点\n" +
            "2. 与领域和目标读者高度相关\n" +
            "3. 不具有时效性\n" +
            "4. 原创且有深度\n" +
            "5. 彼此之间风格多样，避免重复的句式结构\n" +
            "6. 使用多样化的表达方式，如疑问句、陈述句、感叹句等\n" +
            "7. 包含以下类型：\n" +
            "   - 问题型（如：为什么...？如何...？）\n" +
            "   - 数字型（如：5个...技巧，3种...方法）\n" +
            "   - 对比型（如：...vs...，...与...的区别）\n" +
            "   - 故事型（如：一个...的故事，...的传奇）\n" +
            "   - 观点型（如：...的真相，...的误区）\n" +
            "   - 趋势型（如：...的未来，...的发展）\n" +
            "   - 实用型（如：...指南，...手册）\n" +
            "8. 使用生动的动词和形容词\n" +
            "9. 适当使用修辞手法（如比喻、拟人等）\n" +
            "10. 确保每个标题都能激发读者的好奇心和求知欲\n" +
            "请直接返回标题列表，每行一个标题，不要有编号或其他解释文字。\n" +
            "非常重要：确保每次生成的标题都与之前完全不同，避免使用固定模板或相似结构。";
            
        String userPrompt = String.format(
            "请为以下领域和目标读者生成60个高质量的文章标题：\n" +
            "领域：%s\n" +
            "目标读者：%s\n\n" +
            "请确保标题多样化，包含不同类型（如'如何'类、列表类、问题类、观点类等），每个标题的主题互不相同，标题主题要围绕具体某个知识点" +
            "并使用不同的表达方式和结构。当前时间戳：%d", 
            area, reader, System.currentTimeMillis()
        );
        
        Map<String, Object> variables = Map.of(
            "area", area,
            "reader", reader,
            "timestamp", System.currentTimeMillis()
        );
        
        String response = aiService.askLLMWithTemplate(systemPrompt, variables, userPrompt);
        return parseArticleTitles(response);
    }

    /**
     * 解析AI返回的文章标题列表
     * 
     * @param response AI返回的响应
     * @return 解析后的标题列表
     */
    private List<String> parseArticleTitles(String response) {
        if (response == null || response.isEmpty()) {
            logger.warn("AI返回的标题列表为空");
            return new ArrayList<>();
        }
        
        // 按行分割并过滤空行
        return Arrays.stream(response.split("\\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .filter(line -> !line.matches("^\\d+\\..*")) // 过滤掉可能的编号（如"1. 标题"）
                .map(line -> line.replaceAll("^[\\-\\*]\\s+", "")) // 移除可能的列表符号（如"- 标题"）
                .limit(60) // 确保最多返回60个标题
                .collect(Collectors.toList());
    }

    /**
     * 从标题列表中随机选择一个标题
     * 
     * @param titles 标题列表
     * @return 随机选择的标题
     */
    private String selectRandomTitle(List<String> titles) {
        if (titles == null || titles.isEmpty()) {
            logger.warn("标题列表为空，无法选择随机标题");
            return "AIKG每日推送";
        }
        
        int randomIndex = new java.util.Random().nextInt(titles.size());
        return titles.get(randomIndex);
    }

    /**
     * 根据选中的标题生成文章内容提示词
     * 
     * @param area 领域
     * @param reader 读者
     * @param title 选中的标题
     * @return 生成文章内容的提示词
     */
    private String generateContentPrompt(String area, String reader, String title) {
        // 创建一个随机数，用于选择不同的写作风格和结构
        int styleVariant = new java.util.Random().nextInt(8);
        String styleGuidance;
        
        // 根据随机数选择不同的写作风格指导
        switch (styleVariant) {
            case 0:
                styleGuidance = "采用学术论文风格，包含引言、方法、结果和讨论部分，注重逻辑性和严谨性。深入分析问题的本质，提供详实的论据支持。";
                break;
            case 1:
                styleGuidance = "采用故事叙述风格，通过生动的案例和比喻来解释复杂概念，增强可读性。用引人入胜的叙事方式展开内容，让读者身临其境。";
                break;
            case 2:
                styleGuidance = "采用问答形式，预设读者可能的问题并提供深入解答，增强互动性。通过层层递进的问题，引导读者深入思考。";
                break;
            case 3:
                styleGuidance = "采用观点评析风格，提出一个核心论点并从多角度进行分析，展示思考深度。对比不同观点，提供独到见解。";
                break;
            case 4:
                styleGuidance = "采用实用指南风格，提供具体的步骤、方法和建议，注重实用性。包含详细的实施步骤和注意事项。";
                break;
            case 5:
                styleGuidance = "采用对话访谈风格，模拟与领域专家的对话，展示专业见解。通过问答形式深入探讨专业话题。";
                break;
            case 6:
                styleGuidance = "采用案例分析风格，通过具体案例深入分析，提供实践指导。结合理论知识和实践经验。";
                break;
            default:
                styleGuidance = "采用趋势展望风格，分析当前现状并展望未来发展方向。结合历史数据和未来预测。";
                break;
        }
        
        String systemPrompt = 
            "你是一个专业的内容创作者，擅长深度思考和分析。\n" +
            "请以纯文本格式回复，不要使用Markdown或其他格式。\n" +
            "在撰写文章前，请先进行深入思考，确保内容质量高，逻辑清晰，观点深刻。\n" +
            "你的文章应该具有专业性和权威性，同时保持通俗易懂，适合目标读者阅读。\n" +
            "文章结构应清晰，包含引言、主体和结论。\n" +
            "内容不应具有时效性，应该是经得起时间考验的,不能包含数学公式或者程序代码。\n" +
            "非常重要：每次生成的内容必须是独特的、原创的，与之前生成的内容有明显区别。\n" +
            "请使用多样化的表达方式、结构和观点，避免套用固定模板。\n" +
            "尝试从不同角度思考问题，提供新颖的见解和独特的表述。\n" +
            "文章要求：\n" +
            "1. 内容长度：文章总字数不少于2000字\n" +
            "2. 结构要求：\n" +
            "   - 引言：概述主题，提出问题或观点\n" +
            "   - 主体：分为3-5个主要部分，每部分300-500字\n" +
            "   - 结论：总结要点，展望未来\n" +
            "   - 扩展阅读：提供3-5篇相关文章的标题和链接\n" +
            "3. 内容深度：\n" +
            "   - 深入分析问题的本质和根源\n" +
            "   - 提供详实的论据和数据支持\n" +
            "   - 探讨问题的多个维度和层面\n" +
            "   - 分析问题的因果关系\n" +
            "4. 内容广度：\n" +
            "   - 联系相关领域和知识\n" +
            "   - 对比不同观点和方法\n" +
            "   - 提供多个解决方案\n" +
            "   - 考虑不同场景和应用\n" +
            "5. 写作技巧：\n" +
            "   - 使用生动的比喻和类比\n" +
            "   - 加入具体的例子和场景\n" +
            "   - 适当使用修辞手法\n" +
            "   - 设置悬念和引导\n" +
            "   - 增加互动性的提问\n" +
            "   - 使用数据和事实支撑\n" +
            "   - 提供实用的建议和解决方案\n" +
            "   - 注意段落间的过渡和连接\n" +
            "6. 思考元素：\n" +
            "   - 提出深入的问题供读者思考\n" +
            "   - 分析问题的多个角度\n" +
            "   - 探讨可能的解决方案\n" +
            "   - 讨论未来的发展趋势\n" +
            "7. 互动元素：\n" +
            "   - 设置思考问题\n" +
            "   - 提供实践建议\n" +
            "   - 鼓励读者参与讨论\n" +
            "   - 分享个人经验\n" +
            "8. 扩展阅读要求：\n" +
            "   - 在文章末尾添加\"扩展阅读\"部分\n" +
            "   - 提供3-5篇相关文章的标题和链接\n" +
            "   - 每篇文章都应该与当前主题相关但角度不同\n" +
            "   - 链接应该指向权威网站或专业平台\n" +
            "   - 简要说明每篇文章的价值和特点\n" +
            styleGuidance + "\n" +
            "在文章末尾，请注明此文章是由AI生成，读者需自行辨别风险。";
            
        String userPrompt = String.format(
            "请根据以下信息撰写一篇高质量的文章：\n" +
            "领域：%s\n" +
            "目标读者：%s\n" +
            "文章标题：%s\n\n" +
            "要求：\n" +
            "1. 文章内容质量需达到果壳网发表文章的标准\n" +
            "2. 内容要通俗易懂，适合目标读者阅读\n" +
            "3. 内容应该与领域和标题紧密相关\n" +
            "4. 请确保这篇文章与你之前生成的任何内容都不相同\n" +
            "5. 增加文章的趣味性和互动性\n" +
            "6. 使用生动的语言和具体的例子\n" +
            "7. 提供实用的建议和解决方案\n" +
            "8. 文章总字数不少于2000字\n" +
            "9. 深入分析问题的本质和根源\n" +
            "10. 提供详实的论据和数据支持\n" +
            "11. 探讨问题的多个维度和层面\n" +
            "12. 分析问题的因果关系\n" +
            "13. 联系相关领域和知识\n" +
            "14. 对比不同观点和方法\n" +
            "15. 提供多个解决方案\n" +
            "16. 考虑不同场景和应用\n" +
            "17. 在文章末尾添加扩展阅读部分，提供3-5篇相关文章的标题和链接\n" +
            "18. 当前时间戳：%d",
            area, reader, title, System.currentTimeMillis()
        );
        
        Map<String, Object> variables = Map.of(
            "area", area,
            "reader", reader,
            "title", title,
            "timestamp", System.currentTimeMillis(),
            "styleGuidance", styleGuidance
        );
        
        return aiService.askLLMWithTemplate(systemPrompt, variables, userPrompt);
    }


    /**
     * 将Markdown格式文本转换为纯文本格式
     * 
     * 移除Markdown语法标记，保留纯文本内容
     * 
     * @param markdown Markdown格式的文本
     * @return 转换后的纯文本
     */
    private String convertMarkdownToPlainText(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            logger.warn("转换为纯文本的输入为空");
            return markdown;
        }
        
        logger.debug("开始将Markdown转换为纯文本，输入长度: {} 字符", markdown.length());
        String plainText = markdown.replaceAll("```[^`]*```", "") // 移除代码块
                      .replaceAll("\\*\\*(.+?)\\*\\*", "$1") // 移除加粗
                      .replaceAll("\\*(.+?)\\*", "$1") // 移除斜体
                      .replaceAll("#+ (.+)", "$1") // 移除标题标记
                      .replaceAll("\\[(.+?)\\]\\(.+?\\)", "$1") // 移除链接,保留链接文本
                      .replaceAll("^[-*+] ", "") // 移除列表标记
                      .replaceAll("(?m)^\\d+\\. ", "") // 移除有序列表标记
                      .replaceAll("`(.+?)`", "$1") // 移除行内代码
                      .replaceAll("\\n{3,}", "\n\n") // 将多个空行替换为双空行
                      .trim();
        logger.debug("Markdown转换完成，输出长度: {} 字符", plainText.length());
        return plainText;
    }

    /**
     * 监听订阅变更事件
     * 
     * 根据订阅变更类型执行相应的操作：
     * - 创建：为新订阅创建定时任务
     * - 更新：更新订阅的定时任务
     * - 删除：取消订阅的定时任务
     * - 激活：为订阅创建定时任务
     * - 停用：取消订阅的定时任务
     * - 定时任务更新：更新订阅的定时任务
     * 
     * @param event 订阅变更事件
     */
    @EventListener
    public void handleSubscriptionChangeEvent(SubscriptionChangeEvent event) {
        SubscriptionRole subscription = event.getSubscription();
        SubscriptionChangeType type = event.getType();
        
        logger.info("接收到订阅变更事件: ID={}, 类型={}", subscription.getId(), type);
        
        switch (type) {
            case CREATED:
            case UPDATED:
            case ACTIVATED:
            case SCHEDULE_UPDATED:
                // 创建或更新定时任务
                scheduleSubscription(subscription);
                break;
                
            case DELETED:
            case DEACTIVATED:
                // 取消定时任务
                cancelScheduledTask(subscription.getId());
                break;
                
            default:
                logger.warn("未处理的订阅变更类型: {}", type);
        }
    }
} 