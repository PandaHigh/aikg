package com.know.aikg.exception;

/**
 * 安全相关异常类
 */
public class SecurityException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    
    /**
     * 异常错误码
     */
    private final String errorCode;
    
    /**
     * 构造方法
     * @param message 错误消息
     */
    public SecurityException(String message) {
        this("SEC-0001", message);
    }
    
    /**
     * 构造方法
     * @param errorCode 错误码
     * @param message 错误消息
     */
    public SecurityException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    /**
     * 构造方法
     * @param errorCode 错误码
     * @param message 错误消息
     * @param cause 原始异常
     */
    public SecurityException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    /**
     * 获取错误码
     * @return 错误码
     */
    public String getErrorCode() {
        return errorCode;
    }
    
    /**
     * 权限不足异常
     * @param resource 资源名称
     * @return SecurityException实例
     */
    public static SecurityException forbidden(String resource) {
        return new SecurityException("SEC-0002", "没有权限访问资源: " + resource);
    }
    
    /**
     * 认证失败异常
     * @return SecurityException实例
     */
    public static SecurityException unauthorized() {
        return new SecurityException("SEC-0003", "用户未认证或认证失败");
    }
    
    /**
     * 账户锁定异常
     * @param username 用户名
     * @return SecurityException实例
     */
    public static SecurityException accountLocked(String username) {
        return new SecurityException("SEC-0004", "账户已被锁定: " + username);
    }
    
    /**
     * 请求限流异常
     * @param requestCount 当前请求次数
     * @param limit 限制次数
     * @return SecurityException实例
     */
    public static SecurityException rateLimit(int requestCount, int limit) {
        return new SecurityException("SEC-0005", "请求过于频繁，当前请求数: " + requestCount + ", 限制: " + limit);
    }
    
    /**
     * 输入验证失败异常
     * @param field 字段名
     * @param message 错误消息
     * @return SecurityException实例
     */
    public static SecurityException validationFailed(String field, String message) {
        return new SecurityException("SEC-0006", "输入验证失败: " + field + " - " + message);
    }
} 