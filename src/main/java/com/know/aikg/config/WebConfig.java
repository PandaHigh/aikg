package com.know.aikg.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import java.io.IOException;
import java.util.Collections;

/**
 * Web相关配置
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Value("${openai.api.url:https://api.openai.com}")
    private String openaiApiUrl;
    
    @Value("${spring.ai.openai.api-key}")
    private String openaiApiKey;
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:8080", "http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Origin", "Content-Type", "Accept", "Authorization")
                .exposedHeaders("X-Total-Count")
                .allowCredentials(true)
                .maxAge(3600);
    }
    
    /**
     * 创建RestTemplate Bean，添加OpenAI API认证拦截器
     * @return RestTemplate实例
     */
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        
        // 添加认证拦截器
        restTemplate.setInterceptors(
            Collections.singletonList(new ClientHttpRequestInterceptor() {
                @Override
                public ClientHttpResponse intercept(HttpRequest request, byte[] body, 
                                                  ClientHttpRequestExecution execution) throws IOException {
                    HttpHeaders headers = request.getHeaders();
                    
                    // 确保请求URL是OpenAI API URL时才添加认证头
                    if (request.getURI().toString().startsWith(openaiApiUrl)) {
                        // 添加认证头，Deepseek API通常兼容OpenAI的Bearer认证方式
                        headers.add("Authorization", "Bearer " + openaiApiKey);
                        
                        // 确保Content-Type是application/json
                        if (!headers.containsKey("Content-Type")) {
                            headers.add("Content-Type", "application/json");
                        }
                        
                        // 可能需要的额外头信息
                        // headers.add("Deepseek-Version", "2023-05-15"); // 如果Deepseek需要特定版本头
                    }
                    
                    return execution.execute(request, body);
                }
            })
        );
        
        return restTemplate;
    }
    
    /**
     * 创建OpenAI API URL Bean
     * @return OpenAI API URL
     */
    @Bean
    public String openaiApiUrl() {
        return openaiApiUrl;
    }
} 