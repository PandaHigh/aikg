package com.know.aikg.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * AI服务类
 * 
 * 负责与大语言模型进行交互，发送提示词并获取生成的内容
 * 使用Spring AI提供的ChatClient接口与AI模型通信
 * 
 * @Service: 标记该类为Spring服务组件
 */
@Service
public class AIService {
    
    /**
     * 日志记录器
     */
    private static final Logger logger = LoggerFactory.getLogger(AIService.class);
    
    /**
     * Spring AI聊天客户端
     * 用于与AI模型进行通信
     */
    private final ChatClient chatClient;
    
    /**
     * 系统提示词，用于激活模型的深度思考能力
     */
    private static final String SYSTEM_PROMPT = 
        "你是一个专业的内容创作者，擅长深度思考和分析。\n" +
        "请以纯文本格式回复，不要使用Markdown或其他格式。不要使用标题、粗体、列表、表格等Markdown语法。\n" +
        "在回答问题前，请先进行深入思考，确保内容质量高，逻辑清晰，观点深刻。\n" +
        "你的回答应该具有专业性和权威性，同时保持通俗易懂。\n" +
        "非常重要：每次生成的内容必须是独特的、原创的，与之前生成的内容有明显区别。\n" +
        "请使用多样化的表达方式、结构和观点，避免套用固定模板。\n" +
        "尝试从不同角度思考问题，提供新颖的见解和独特的表述。";

    /**
     * 构造函数，通过依赖注入获取ChatClient实例
     * 
     * @param chatClient Spring AI聊天客户端
     */
    public AIService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * 向大语言模型发送请求并获取响应
     * 
     * 发送提示词到AI模型并返回生成的内容
     * 记录调用过程中的性能指标和可能的错误
     * 
     * @param question 提示词/问题内容
     * @return AI模型生成的响应内容
     * @throws RuntimeException 当AI调用失败时抛出异常
     */
    public String askLLM(String question) {
        logger.info("开始调用AI模型生成内容");
        logger.debug("AI查询提示词长度: {} 字符", question.length());
        
        try {
            // 记录调用开始时间
            long startTime = System.currentTimeMillis();
            
            // 创建消息列表
            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage(SYSTEM_PROMPT + "\n当前时间戳: " + System.currentTimeMillis() + "\n非常重要：请以纯文本格式回复，不要使用任何Markdown格式。")); // 添加时间戳增加随机性
            messages.add(new UserMessage(question));
            
            // 创建提示词
            Prompt prompt = new Prompt(messages);
            
            // 调用AI模型
            ChatResponse response = chatClient.call(prompt);
            String content = response.getResult().getOutput().getContent();
            
            // 记录调用结束时间
            long endTime = System.currentTimeMillis();
            
            // 记录调用成功信息和性能指标
            logger.info("AI模型调用成功，耗时: {} 毫秒", (endTime - startTime));
            logger.debug("AI响应内容长度: {} 字符", content.length());
            
            return content;
        } catch (Exception e) {
            // 记录调用失败信息
            logger.error("AI模型调用失败，错误: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 使用模板化系统提示词向大语言模型发送请求
     * 
     * 此方法允许使用模板和变量来构建系统提示词
     * 
     * @param systemPromptTemplate 系统提示词模板
     * @param variables 模板变量
     * @param userPrompt 用户提示词
     * @return AI模型生成的响应内容
     */
    public String askLLMWithTemplate(String systemPromptTemplate, Map<String, Object> variables, String userPrompt) {
        logger.info("开始使用模板调用AI模型生成内容");
        logger.debug("系统提示词模板长度: {} 字符, 用户提示词长度: {} 字符", 
                systemPromptTemplate.length(), userPrompt.length());
        
        try {
            // 记录调用开始时间
            long startTime = System.currentTimeMillis();
            
            // 创建变量Map的可变副本
            Map<String, Object> mutableVariables = new HashMap<>(variables);
            
            // 添加当前时间戳作为随机种子，进一步增加多样性
            mutableVariables.put("timestamp", System.currentTimeMillis());
            
            // 添加明确的格式化指令
            if (!systemPromptTemplate.contains("不要使用Markdown")) {
                systemPromptTemplate += "\n请以纯文本格式回复，不要使用Markdown或其他格式。不要使用标题、粗体、列表、表格等Markdown语法。";
            }
            
            // 创建系统提示词
            SystemPromptTemplate template = new SystemPromptTemplate(systemPromptTemplate);
            Message systemMessage = template.createMessage(mutableVariables);
            
            // 创建消息列表
            List<Message> messages = new ArrayList<>();
            messages.add(systemMessage);
            messages.add(new UserMessage(userPrompt));
            
            // 创建提示词
            Prompt prompt = new Prompt(messages);
            
            // 调用AI模型
            ChatResponse response = chatClient.call(prompt);
            String content = response.getResult().getOutput().getContent();
            
            // 记录调用结束时间
            long endTime = System.currentTimeMillis();
            
            // 记录调用成功信息和性能指标
            logger.info("AI模型模板调用成功，耗时: {} 毫秒", (endTime - startTime));
            logger.debug("AI响应内容长度: {} 字符", content.length());
            
            return content;
        } catch (Exception e) {
            // 记录调用失败信息
            logger.error("AI模型模板调用失败，错误: {}", e.getMessage(), e);
            throw e;
        }
    }
} 