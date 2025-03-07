package com.know.aikg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AIKG应用程序的主入口类
 * 
 * @SpringBootApplication: Spring Boot的主要注解，包含了@Configuration, @EnableAutoConfiguration和@ComponentScan
 * @EnableScheduling: 启用Spring的定时任务调度功能
 */
@SpringBootApplication
@EnableScheduling
public class AikgApplication {
	
	/**
	 * 应用程序日志记录器
	 */
	private static final Logger logger = LoggerFactory.getLogger(AikgApplication.class);

	/**
	 * 应用程序入口方法
	 * 
	 * @param args 命令行参数
	 */
	public static void main(String[] args) {
		logger.info("启动 AIKG 应用程序...");
		// 使用SpringApplicationBuilder构建并启动应用程序
		// WebApplicationType.SERVLET指定应用程序为Web应用
		new SpringApplicationBuilder(AikgApplication.class)
			.web(WebApplicationType.SERVLET)
			.run(args);
		logger.info("AIKG 应用程序已成功启动");
	}

}
