package com.know.aikg.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Configuration
public class RateLimitConfig implements WebMvcConfigurer {

    @Bean
    public RateLimitInterceptor rateLimitInterceptor() {
        return new RateLimitInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor())
                .addPathPatterns("/api/**"); // 对API请求进行限流
    }

    /**
     * 请求限流拦截器
     */
    public static class RateLimitInterceptor implements org.springframework.web.servlet.HandlerInterceptor {
        // 使用ConcurrentHashMap存储IP和请求次数
        private final ConcurrentHashMap<String, RequestCount> requestCounts = new ConcurrentHashMap<>();
        // 限制：每分钟60次请求
        private static final int REQUESTS_PER_MINUTE = 60;

        @Override
        public boolean preHandle(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, Object handler) throws Exception {
            // 获取客户端IP
            String clientIp = getClientIp(request);
            
            // 获取当前时间戳
            long currentTime = System.currentTimeMillis();
            
            // 获取或创建请求计数器
            RequestCount count = requestCounts.computeIfAbsent(clientIp, k -> new RequestCount());
            
            // 检查是否超过限制
            if (currentTime - count.getResetTime() > TimeUnit.MINUTES.toMillis(1)) {
                // 如果已经过了1分钟，重置计数器
                count.setCount(1);
                count.setResetTime(currentTime);
            } else {
                // 如果当前请求数超过限制
                if (count.getCount() >= REQUESTS_PER_MINUTE) {
                    response.setStatus(429); // 返回"Too Many Requests"状态码
                    response.setContentType("application/json");
                    response.getWriter().write("{\"message\":\"请求过于频繁，请稍后再试\"}");
                    return false;
                }
                // 增加请求计数
                count.incrementCount();
            }
            
            return true;
        }
        
        /**
         * 获取客户端真实IP
         */
        private String getClientIp(jakarta.servlet.http.HttpServletRequest request) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }
            
            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty()) {
                return xRealIp;
            }
            
            return request.getRemoteAddr();
        }
        
        /**
         * 请求计数器类
         */
        private static class RequestCount {
            private int count;
            private long resetTime;
            
            public RequestCount() {
                this.count = 0;
                this.resetTime = System.currentTimeMillis();
            }
            
            public int getCount() {
                return count;
            }
            
            public void setCount(int count) {
                this.count = count;
            }
            
            public void incrementCount() {
                this.count++;
            }
            
            public long getResetTime() {
                return resetTime;
            }
            
            public void setResetTime(long resetTime) {
                this.resetTime = resetTime;
            }
        }
    }
} 