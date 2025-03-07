package com.know.aikg.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 将根路径重定向到index.html
        registry.addViewController("/").setViewName("forward:/index.html");
        
        // 处理单页应用的刷新问题，使得非API路径都转向index.html
        registry.addViewController("/{x:[\\w\\-]+}")
                .setViewName("forward:/index.html");
        
        // 修复无效的映射模式，使用更简单的路由处理方式
        registry.addViewController("/webapp/**")
                .setViewName("forward:/index.html");
                
        // 处理geoserver相关的请求路径，将它们转发到index.html
        registry.addViewController("/geoserver/**")
                .setViewName("forward:/index.html");
    }
} 