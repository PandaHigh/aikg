package com.know.aikg.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.context.ApplicationEventPublisher;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.know.aikg.event.ArticleScrapingEvent;
import com.know.aikg.event.ArticleSaveEvent;
import com.know.aikg.entity.WebsiteConfig;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.Comparator;
import java.util.concurrent.Executors;

@Service
public class ArticleScraperService {
    private static final Logger logger = LoggerFactory.getLogger(ArticleScraperService.class);
    private final ResourceLoader resourceLoader;
    private final AIService aiService;
    private final String pythonPath;
    private final boolean installDependencies;
    private final String targetUrl;
    private final String baseDomain;
    private final ObjectMapper objectMapper;
    
    @Autowired
    private TaskScheduler taskScheduler;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private String openaiApiUrl;
    
    @Autowired
    private WebsiteConfigService websiteConfigService;

    public ArticleScraperService(
            ResourceLoader resourceLoader,
            AIService aiService,
            @Value("${python.path:/opt/homebrew/bin/python3}") String pythonPath,
            @Value("${python.install-dependencies:true}") boolean installDependencies,
            @Value("${scraper.target-url}") String targetUrl,
            @Value("${scraper.base-domain}") String baseDomain) {
        this.resourceLoader = resourceLoader;
        this.aiService = aiService;
        this.pythonPath = pythonPath;
        this.installDependencies = installDependencies;
        this.targetUrl = targetUrl;
        this.baseDomain = baseDomain;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 应用启动后5秒执行文章爬取任务
     */
    @EventListener(ApplicationReadyEvent.class)
    public void scheduleArticleScraping() {
        logger.info("系统启动完成，将在5秒后执行文章爬取任务...");
        Date startTime = Date.from(Instant.now().plusSeconds(5));
        taskScheduler.schedule(this::executeArticleScrapingForAllWebsites, startTime);
    }
    
    /**
     * 每天凌晨0点重置所有网站的今日爬取尝试次数
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void resetDailyRetryCount() {
        logger.info("开始重置所有网站的今日爬取尝试次数...");
        // 重置所有网站的今日爬取尝试次数
        websiteConfigService.resetAllTodayRetryCount();
        logger.info("重置所有网站的今日爬取尝试次数完成");
    }
    
    /**
     * 每隔10分钟执行一次爬取任务，只爬取今日未成功的网站
     */
    @Scheduled(cron = "0 */10 * * * ?")
    public void scheduledArticleScraping() {
        logger.info("开始执行定时爬取任务...");
        executeArticleScrapingForPendingWebsites();
    }
    
    /**
     * 为所有启用且当天未成功爬取的网站执行文章爬取任务
     */
    private void executeArticleScrapingForPendingWebsites() {
        logger.info("开始为所有启用且当天未成功爬取的网站执行文章爬取任务...");
        List<WebsiteConfig> websites = websiteConfigService.findPendingWebsites();
        
        if (websites.isEmpty()) {
            logger.info("没有需要爬取的网站或所有网站今日已成功爬取");
            return;
        }
        
        logger.info("找到 {} 个需要爬取的网站配置", websites.size());
        for (WebsiteConfig website : websites) {
            executeArticleScraping(website);
        }
    }
    
    /**
     * 每十分钟检查并重试失败的网站爬取
     */
    @Scheduled(cron = "0 */10 * * * ?")
    public void retryFailedWebsites() {
        logger.info("开始检查并重试失败的网站爬取...");
        List<WebsiteConfig> failedWebsites = websiteConfigService.findFailedWithRetryAvailable();
        
        if (failedWebsites.isEmpty()) {
            logger.info("没有需要重试的失败网站");
            return;
        }
        
        logger.info("发现 {} 个失败网站需要重试", failedWebsites.size());
        for (WebsiteConfig website : failedWebsites) {
            executeArticleScraping(website);
        }
    }
    
    /**
     * 为所有启用的网站执行文章爬取任务（包括今日已成功的网站）
     */
    private void executeArticleScrapingForAllWebsites() {
        logger.info("开始为所有启用的网站执行文章爬取任务...");
        List<WebsiteConfig> websites = websiteConfigService.findEnabledWithRetryAvailable();
        
        if (websites.isEmpty()) {
            logger.info("没有启用的网站配置或所有网站今日已达到最大重试次数");
            return;
        }
        
        logger.info("找到 {} 个启用的网站配置", websites.size());
        for (WebsiteConfig website : websites) {
            executeArticleScraping(website);
        }
    }
    
    /**
     * 执行文章爬取任务
     */
    private void executeArticleScraping() {
        logger.info("开始执行定时文章爬取任务...");
        try {
            // 获取文章列表
            List<Map<String, String>> articles = scrapeArticleList(targetUrl, baseDomain);
            logger.info("文章爬取任务完成，成功爬取 {} 篇文章", articles.size());
            
            // 发布文章保存事件
            eventPublisher.publishEvent(new ArticleSaveEvent(this, articles, 1L)); // 使用默认订阅角色ID
            
            // 打印所有文章摘要的总结
            logger.info("===== 文章列表总结 =====");
            for (int i = 0; i < articles.size(); i++) {
                Map<String, String> article = articles.get(i);
                logger.info("文章 {}/{}: 标题=\"{}\", 链接=\"{}\"", 
                        i+1, articles.size(), article.get("title"), article.get("url"));
            }
            logger.info("===== 文章列表总结结束 =====");
            
            // 发布文章爬取完成事件
            eventPublisher.publishEvent(new ArticleScrapingEvent(this, true, articles.size()));
        } catch (Exception e) {
            logger.error("定时文章爬取任务执行失败", e);
            // 发布文章爬取失败事件
            eventPublisher.publishEvent(new ArticleScrapingEvent(this, false, 0));
        }
    }
    
    /**
     * 为特定网站执行文章爬取任务
     * 
     * @param website 网站配置
     */
    public void executeArticleScraping(WebsiteConfig website) {
        logger.info("开始为网站 ID: {}, 名称: {}, URL: {} 执行文章爬取任务...", 
                website.getId(), website.getName(), website.getUrl());
        try {
            // 获取文章列表
            List<Map<String, String>> articles = scrapeArticleList(
                    website.getUrl(), website.getBaseDomain());
            
            logger.info("网站 {} 文章爬取任务完成，成功爬取 {} 篇文章", website.getName(), articles.size());
            
            // 发布文章保存事件
            eventPublisher.publishEvent(new ArticleSaveEvent(this, articles, 1L)); // 使用默认订阅角色ID
            
            // 打印所有文章摘要的总结
            logger.info("===== 网站 {} 文章列表总结 =====", website.getName());
            for (int i = 0; i < articles.size(); i++) {
                Map<String, String> article = articles.get(i);
                logger.info("文章 {}/{}: 标题=\"{}\", 链接=\"{}\"", 
                        i+1, articles.size(), article.get("title"), article.get("url"));
            }
            logger.info("===== 网站 {} 文章列表总结结束 =====", website.getName());
            
            // 更新网站爬取状态为成功
            websiteConfigService.updateToSuccess(website.getId());
            
            // 发布文章爬取完成事件
            eventPublisher.publishEvent(new ArticleScrapingEvent(this, true, articles.size()));
        } catch (Exception e) {
            logger.error("网站 {} 文章爬取任务执行失败: {}", website.getName(), e.getMessage());
            // 更新网站爬取状态为失败
            websiteConfigService.updateToFailed(website.getId());
            // 发布文章爬取失败事件
            eventPublisher.publishEvent(new ArticleScrapingEvent(this, false, 0));
        }
    }

    /**
     * 爬取文章列表
     * @param url 网站URL
     * @param baseDomain 网站基础域名
     * @return 包含文章信息的列表
     */
    private List<Map<String, String>> scrapeArticleList(String url, String baseDomain) {
        try {
            // 1. 确保Python脚本存在
            Resource scriptResource = resourceLoader.getResource("classpath:scripts/article_scraper.py");
            if (!scriptResource.exists()) {
                throw new RuntimeException("找不到Python爬虫脚本");
            }

            // 2. 创建临时目录存放脚本和结果
            Path tempDir = Files.createTempDirectory("article_scraper");
            Path scriptPath = tempDir.resolve("article_scraper.py");
            Path mainHtmlPath = tempDir.resolve("main_page.html");
            Files.copy(scriptResource.getInputStream(), scriptPath);

            // 3. 第一步：使用Python获取主页HTML内容
            logger.info("开始获取主页HTML内容: {}", url);
            String mainPageHtml = fetchHtmlContent(scriptPath, url, mainHtmlPath, false);
            if (mainPageHtml == null || mainPageHtml.isEmpty()) {
                throw new RuntimeException("无法获取主页HTML内容");
            }
            logger.info("成功获取主页HTML内容，大小: {} 字节", mainPageHtml.length());

            // 4. 第二步：使用大模型提取文章列表
            logger.info("开始使用大模型提取文章列表");
            List<Map<String, String>> articles = extractArticlesWithLLM(mainPageHtml);
            
            if (articles.isEmpty()) {
                logger.warn("未提取到任何文章，任务结束");
                return new ArrayList<>();
            }
            
            logger.info("大模型提取到 {} 篇文章", articles.size());
            
            // 添加文章来源URL
            for (Map<String, String> article : articles) {
                article.put("source_url", url);
                
                // 如果URL是相对路径，则拼接基础域名
                String articleUrl = article.get("url");
                if (articleUrl != null && !articleUrl.startsWith("http")) {
                    if (articleUrl.startsWith("/")) {
                        article.put("url", baseDomain + articleUrl);
                    } else {
                        article.put("url", baseDomain + "/" + articleUrl);
                    }
                }
            }

            // 5. 清理临时文件
            cleanupTempFiles(tempDir);

            return articles;
        } catch (Exception e) {
            logger.error("文章列表爬取失败", e);
            throw new RuntimeException("文章列表爬取失败", e);
        }
    }

    /**
     * 使用Python获取HTML内容
     * 
     * @param scriptPath Python脚本路径
     * @param url 要获取的URL
     * @param outputPath 输出文件路径
     * @param stripAllTags 是否移除所有HTML标签，仅保留文本内容
     * @return 获取的HTML内容
     */
    private String fetchHtmlContent(Path scriptPath, String url, Path outputPath, boolean stripAllTags) throws Exception {
        // 添加1秒的延迟，避免请求过于频繁
        TimeUnit.SECONDS.sleep(1);
        
        List<String> command = new ArrayList<>();
        command.add(pythonPath);
        command.add(scriptPath.toString());
        command.add("--url");
        command.add(url);
        command.add("--output");
        command.add(outputPath.toString());
        
        // 如果需要移除所有标签，添加对应的参数
        if (stripAllTags) {
            command.add("--strip-all-tags");
        }
        
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // 读取Python脚本的输出
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logger.info("Python输出: {}", line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            logger.error("Python脚本执行失败，退出码: {}", exitCode);
            return null;
        }

        if (!Files.exists(outputPath)) {
            logger.error("找不到HTML输出文件: {}", outputPath);
            return null;
        }

        return new String(Files.readAllBytes(outputPath), StandardCharsets.UTF_8);
    }

    /**
     * 使用大模型提取文章列表
     */
    private List<Map<String, String>> extractArticlesWithLLM(String htmlContent) {
        String systemPrompt = 
            "你是一个专业的HTML解析助手。\n" +
            "你的任务是从HTML内容中提取所有文章标题和链接。\n" +
            "提取页面中所有看起来像文章的链接。\n" +
            "请注意以下几点：\n" +
            "1. 关注那些看起来像政策文件、通知、公告等官方文档的链接\n" +
            "2. 忽略导航链接、功能按钮等非文章内容\n" +
            "3. 确保提取的URL是完整的，如果是相对路径，请根据基础域名补全\n" +
            "请按照以下格式返回结果（仅返回JSON格式，不要有其他文字）：\n" +
            "[\n" +
            "  {\n" +
            "    \"title\": \"文章标题1\",\n" +
            "    \"url\": \"文章链接1\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"title\": \"文章标题2\",\n" +
            "    \"url\": \"文章链接2\"\n" +
            "  }\n" +
            "]\n";

        String userPrompt = String.format(
            "请从以下HTML内容中提取文章标题和链接，提取页面中所有看起来像文章的链接：\n\n%s", 
            htmlContent
        );

        logger.info("开始使用大模型提取文章列表，HTML内容大小: {} 字节", htmlContent.length());
        
        // 重试机制
        int maxRetries = 3;
        int retryCount = 0;
        long retryDelayMs = 1000; // 初始延迟1秒
        String response = null;
        
        while (retryCount < maxRetries) {
            try {
                logger.info("正在提取文章列表，尝试次数: {}/{}", retryCount + 1, maxRetries);
                
                // 调用AI服务
                response = aiService.askLLM(systemPrompt + "\n\n" + userPrompt);
                
                if (response == null || response.trim().isEmpty()) {
                    logger.warn("AI返回的响应为空，尝试重试");
                    retryCount++;
                    Thread.sleep(retryDelayMs);
                    retryDelayMs *= 2; // 指数退避
                    continue;
                }
                
                // 尝试解析JSON响应
                List<Map<String, String>> articles = objectMapper.readValue(response, new TypeReference<List<Map<String, String>>>() {});
                
                // 验证提取的文章
                if (articles.isEmpty()) {
                    logger.warn("未提取到任何文章，尝试重试");
                    retryCount++;
                    Thread.sleep(retryDelayMs);
                    retryDelayMs *= 2; // 指数退避
                    continue;
                }
                
                // 处理URL，确保都是完整的URL
                for (Map<String, String> article : articles) {
                    String url = article.get("url");
                    if (url != null && !url.startsWith("http")) {
                        // 如果URL是相对路径，添加基础域名
                        if (url.startsWith("/")) {
                            article.put("url", baseDomain + url);
                        } else {
                            article.put("url", baseDomain + "/" + url);
                        }
                    }
                    
                    // 确保标题不为空
                    if (article.get("title") == null || article.get("title").trim().isEmpty()) {
                        article.put("title", "未知标题");
                    }
                }
                
                logger.info("成功提取到 {} 篇文章", articles.size());
                return articles;
                
            } catch (Exception e) {
                logger.error("提取文章列表时发生错误: {}", e.getMessage());
                logger.debug("AI响应内容: {}", response);
                
                // 重试
                retryCount++;
                try {
                    logger.info("等待 {} 毫秒后重试...", retryDelayMs);
                    Thread.sleep(retryDelayMs);
                    retryDelayMs *= 2; // 指数退避
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logger.error("重试等待被中断", ie);
                    break;
                }
            }
        }
        
        // 所有重试都失败，返回空列表
        logger.warn("在 {} 次尝试后仍无法提取文章列表", maxRetries);
        return new ArrayList<>();
    }

    /**
     * 清理临时文件和目录
     */
    private void cleanupTempFiles(Path tempDir) {
        if (tempDir == null || !Files.exists(tempDir)) {
            return;
        }
        
        try {
            Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception e) {
                        logger.warn("删除临时文件失败: {}", path, e);
                    }
                });
            logger.info("临时文件清理完成");
        } catch (Exception e) {
            logger.error("清理临时文件时发生错误", e);
        }
    }

    /**
     * 获取文章内容
     * @param url 文章URL
     * @return 包含纯文本内容的Map
     */
    public Map<String, String> fetchArticleContent(String url) {
        try {
            // 确保Python脚本存在
            Resource scriptResource = resourceLoader.getResource("classpath:scripts/article_scraper.py");
            if (!scriptResource.exists()) {
                throw new RuntimeException("找不到Python爬虫脚本");
            }

            // 创建临时目录存放脚本和结果
            Path tempDir = Files.createTempDirectory("article_content");
            Path scriptPath = tempDir.resolve("article_scraper.py");
            Files.copy(scriptResource.getInputStream(), scriptPath);
            
            // 获取文章纯文本内容（移除所有HTML标签）
            Path articleTextPath = tempDir.resolve("article_text.txt");
            String articleText = fetchHtmlContent(scriptPath, url, articleTextPath, true);
            
            // 清理临时文件
            cleanupTempFiles(tempDir);
            
            Map<String, String> result = new HashMap<>();
            result.put("text_content", articleText);
            
            return result;
        } catch (Exception e) {
            logger.error("获取文章内容失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 生成文章摘要
     * @param title 文章标题
     * @param url 文章URL
     * @return 生成的摘要
     */
    public String generateSummary(String title, String url) {
        try {
            // 根据标题和URL生成摘要
            String systemPrompt = "你是一个专业的文章摘要生成器。请根据提供的文章标题和URL，生成一个简短的摘要。摘要应该简洁明了，不超过300字，并说明这是基于标题和URL生成的预览摘要。";
            
            String userPrompt = String.format("文章标题：%s\n文章URL：%s\n请根据这些信息生成一个简短的摘要。", title, url);
            
            // 重试机制
            int maxRetries = 3;
            int retryCount = 0;
            long retryDelayMs = 1000; // 初始延迟1秒
            
            while (retryCount < maxRetries) {
                try {
                    logger.info("正在生成摘要，尝试次数: {}/{}", retryCount + 1, maxRetries);
                    
                    // 构建请求体
                    Map<String, Object> requestBody = new HashMap<>();
                    requestBody.put("model", "gpt-3.5-turbo");
                    
                    List<Map<String, String>> messages = new ArrayList<>();
                    messages.add(Map.of("role", "system", "content", systemPrompt));
                    messages.add(Map.of("role", "user", "content", userPrompt));
                    
                    requestBody.put("messages", messages);
                    requestBody.put("temperature", 0.3); // 降低温度以获得更确定性的输出
                    requestBody.put("max_tokens", 500); // 限制输出token数量
                    
                    // 发送请求
                    ResponseEntity<Map> response = restTemplate.postForEntity(
                            openaiApiUrl + "/v1/chat/completions",
                            requestBody,
                            Map.class
                    );
                    
                    // 处理响应
                    if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                        Map<String, Object> responseBody = response.getBody();
                        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                        
                        if (choices != null && !choices.isEmpty()) {
                            Map<String, Object> choice = choices.get(0);
                            Map<String, String> message = (Map<String, String>) choice.get("message");
                            
                            if (message != null && message.containsKey("content")) {
                                String summary = message.get("content").trim();
                                
                                // 检查摘要是否为空
                                if (summary.isEmpty()) {
                                    logger.warn("AI返回的摘要为空，尝试重试");
                                    retryCount++;
                                    Thread.sleep(retryDelayMs);
                                    retryDelayMs *= 2; // 指数退避
                                    continue;
                                }
                                
                                logger.info("摘要生成成功，长度: {}", summary.length());
                                return summary;
                            }
                        }
                    }
                    
                    // 如果到达这里，说明响应格式不符合预期
                    logger.warn("AI响应格式不符合预期: {}", response.getBody());
                    
                } catch (Exception e) {
                    logger.error("生成摘要时发生错误: {}", e.getMessage(), e);
                }
                
                // 重试
                retryCount++;
                try {
                    logger.info("等待 {} 毫秒后重试...", retryDelayMs);
                    Thread.sleep(retryDelayMs);
                    retryDelayMs *= 2; // 指数退避
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logger.error("重试等待被中断", ie);
                    break;
                }
            }
            
            // 所有重试都失败，返回默认摘要
            logger.warn("在 {} 次尝试后仍无法生成摘要", maxRetries);
            return String.format("这是一篇标题为\"%s\"的文章。由于技术原因，无法生成详细摘要。请直接查看原文获取更多信息。", title);
        } catch (Exception e) {
            logger.error("生成摘要失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 根据文章内容生成摘要
     * @param content 文章内容
     * @param title 文章标题
     * @return 生成的摘要
     */
    public String generateSummaryFromContent(String content, String title) {
        try {
            // 检查内容是否为空
            if (content == null || content.trim().isEmpty()) {
                logger.warn("无法生成摘要：内容为空");
                return String.format("这是一篇标题为\"%s\"的文章。由于内容为空，无法生成详细摘要。请直接查看原文获取更多信息。", title);
            }

            // 限制内容长度，避免超出模型的token限制
            // 大约15000个字符对应约3000个token，这是一个安全的限制
            final int MAX_CONTENT_LENGTH = 15000;
            String truncatedContent = content;
            boolean contentTruncated = false;
            
            if (content.length() > MAX_CONTENT_LENGTH) {
                truncatedContent = content.substring(0, MAX_CONTENT_LENGTH) + "...（内容已截断）";
                contentTruncated = true;
                logger.info("内容已截断，原始长度: {}, 截断后长度: {}", content.length(), truncatedContent.length());
            }
            
            // 设置系统提示
            String systemPrompt = "你是一个专业的文章摘要生成器。请从提供的文本内容中提取关键信息，生成一个客观、准确的摘要。摘要应该简洁明了，不超过300字，不要包含个人观点或评价。";
            
            if (contentTruncated) {
                systemPrompt += " 注意：由于内容过长已被截断，请基于可用内容生成最佳摘要。";
            }
            
            // 设置用户提示
            String userPrompt = String.format("文章标题：%s\n\n以下是文章的文本内容，请生成摘要：\n\n%s", title, truncatedContent);
            
            // 重试机制
            int maxRetries = 3;
            int retryCount = 0;
            long retryDelayMs = 1000; // 初始延迟1秒
            
            while (retryCount < maxRetries) {
                try {
                    logger.info("正在生成摘要，尝试次数: {}/{}", retryCount + 1, maxRetries);
                    
                    // 构建请求体
                    Map<String, Object> requestBody = new HashMap<>();
                    // 使用Deepseek兼容的模型名称
                    requestBody.put("model", "deepseek-chat");
                    
                    List<Map<String, String>> messages = new ArrayList<>();
                    messages.add(Map.of("role", "system", "content", systemPrompt));
                    messages.add(Map.of("role", "user", "content", userPrompt));
                    
                    requestBody.put("messages", messages);
                    requestBody.put("temperature", 0.3); // 降低温度以获得更确定性的输出
                    requestBody.put("max_tokens", 500); // 限制输出token数量
                    
                    // 记录请求详情
                    logger.debug("API请求URL: {}", openaiApiUrl + "/v1/chat/completions");
                    logger.debug("API请求体: {}", requestBody);
                    
                    // 发送请求
                    ResponseEntity<Map> response = restTemplate.postForEntity(
                            openaiApiUrl + "/v1/chat/completions",
                            requestBody,
                            Map.class
                    );
                    
                    // 记录响应状态
                    logger.debug("API响应状态: {}", response.getStatusCode());
                    
                    // 处理响应
                    if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                        logger.debug("API响应体: {}", response.getBody());
                        
                        Map<String, Object> responseBody = response.getBody();
                        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                        
                        if (choices != null && !choices.isEmpty()) {
                            Map<String, Object> choice = choices.get(0);
                            Map<String, String> message = (Map<String, String>) choice.get("message");
                            
                            if (message != null && message.containsKey("content")) {
                                String summary = message.get("content").trim();
                                
                                // 检查摘要是否为空
                                if (summary.isEmpty()) {
                                    logger.warn("AI返回的摘要为空，尝试重试");
                                    retryCount++;
                                    Thread.sleep(retryDelayMs);
                                    retryDelayMs *= 2; // 指数退避
                                    continue;
                                }
                                
                                logger.info("摘要生成成功，长度: {}", summary.length());
                                return summary;
                            } else {
                                logger.warn("API响应中没有找到message.content字段");
                            }
                        } else {
                            logger.warn("API响应中没有找到choices字段或choices为空");
                        }
                    } else {
                        logger.warn("API响应不成功或响应体为空: {}", response);
                    }
                    
                    // 如果到达这里，说明响应格式不符合预期
                    logger.warn("AI响应格式不符合预期: {}", response.getBody());
                    
                } catch (Exception e) {
                    logger.error("生成摘要时发生错误: {}", e.getMessage(), e);
                    // 记录更详细的错误信息
                    if (e.getCause() != null) {
                        logger.error("错误原因: {}", e.getCause().getMessage());
                    }
                }
                
                // 重试
                retryCount++;
                try {
                    logger.info("等待 {} 毫秒后重试...", retryDelayMs);
                    Thread.sleep(retryDelayMs);
                    retryDelayMs *= 2; // 指数退避
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logger.error("重试等待被中断", ie);
                    break;
                }
            }
            
            // 所有重试都失败，返回默认摘要
            logger.warn("在 {} 次尝试后仍无法生成摘要", maxRetries);
            return String.format("这是一篇标题为\"%s\"的文章。由于技术原因，无法生成详细摘要。请直接查看原文获取更多信息。", title);
        } catch (Exception e) {
            logger.error("生成摘要失败: {}", e.getMessage(), e);
            return null;
        }
    }
} 