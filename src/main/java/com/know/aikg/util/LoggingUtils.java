package com.know.aikg.util;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.MDC;

/**
 * 日志工具类，提供日志增强功能
 */
public class LoggingUtils {

    private static final String REQUEST_ID_KEY = "requestId";
    private static final String USER_KEY = "user";
    private static final String ACTION_KEY = "action";

    /**
     * 生成并设置请求ID到MDC
     * @return 生成的请求ID
     */
    public static String generateRequestId() {
        String requestId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        MDC.put(REQUEST_ID_KEY, requestId);
        return requestId;
    }

    /**
     * 清除MDC中的所有数据
     */
    public static void clearMDC() {
        MDC.clear();
    }

    /**
     * 设置当前用户到MDC
     * @param user 用户标识
     */
    public static void setUser(String user) {
        if (user != null) {
            MDC.put(USER_KEY, user);
        }
    }

    /**
     * 设置当前操作到MDC
     * @param action 操作名称
     */
    public static void setAction(String action) {
        if (action != null) {
            MDC.put(ACTION_KEY, action);
        }
    }

    /**
     * 记录方法开始执行日志
     * @param logger 日志记录器
     * @param methodName 方法名
     * @param args 方法参数
     */
    public static void logMethodEntry(Logger logger, String methodName, Object... args) {
        if (!logger.isDebugEnabled()) {
            return;
        }
        
        StringBuilder message = new StringBuilder();
        message.append("[方法开始] ").append(methodName);
        
        if (args != null && args.length > 0) {
            message.append(" 参数: [");
            for (int i = 0; i < args.length; i++) {
                if (i > 0) {
                    message.append(", ");
                }
                
                // 对敏感信息进行脱敏处理
                String argStr = getSafeString(args[i]);
                message.append(argStr);
            }
            message.append("]");
        }
        
        logger.debug(message.toString());
    }

    /**
     * 记录方法执行结束日志
     * @param logger 日志记录器
     * @param methodName 方法名
     * @param startTime 开始时间
     * @param result 方法返回结果
     */
    public static void logMethodExit(Logger logger, String methodName, long startTime, Object result) {
        if (!logger.isDebugEnabled()) {
            return;
        }
        
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        
        StringBuilder message = new StringBuilder();
        message.append("[方法结束] ").append(methodName)
               .append(" 耗时: ").append(executionTime).append("ms");
        
        if (result != null) {
            // 对敏感信息进行脱敏处理
            String resultStr = getSafeString(result);
            message.append(" 结果: ").append(resultStr);
        }
        
        logger.debug(message.toString());
    }

    /**
     * 记录方法异常日志
     * @param logger 日志记录器
     * @param methodName 方法名
     * @param e 异常
     */
    public static void logMethodException(Logger logger, String methodName, Exception e) {
        logger.error("[方法异常] " + methodName + " 错误: " + e.getMessage(), e);
    }

    /**
     * 对敏感信息进行处理，避免泄露
     * @param obj 要处理的对象
     * @return 安全的字符串表示
     */
    private static String getSafeString(Object obj) {
        if (obj == null) {
            return "null";
        }
        
        String str = obj.toString();
        
        // 对敏感信息进行脱敏处理
        if (str.contains("password")) {
            return "******";
        }
        
        // 对邮箱地址进行脱敏处理
        if (str.matches(".*[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}.*")) {
            return str.replaceAll("([a-zA-Z0-9._%+-]+)@([a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6})", "****@$2");
        }
        
        // 对超长字符串进行截断
        if (str.length() > 100) {
            return str.substring(0, 100) + "... (总长度: " + str.length() + ")";
        }
        
        return str;
    }
} 