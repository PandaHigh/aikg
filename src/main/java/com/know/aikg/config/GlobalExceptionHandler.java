package com.know.aikg.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.know.aikg.exception.SecurityException;

@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 通用异常处理
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleAllExceptions(Exception ex, WebRequest request) {
        logger.error("Unexpected error occurred", ex);
        Map<String, String> errorResponse = new HashMap<>();
        // 生产环境不应该返回具体的错误信息，防止信息泄露
        errorResponse.put("message", "服务器内部错误，请联系管理员");
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // 运行时异常处理
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex, WebRequest request) {
        logger.error("Runtime exception occurred", ex);
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("message", "请求处理失败，请稍后重试");
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // 参数验证异常处理
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        logger.warn("Invalid argument", ex);
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("message", ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    // 表单验证异常处理
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        logger.warn("Validation failed", ex);
        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "验证错误",
                        (error1, error2) -> error1
                ));
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "输入验证失败");
        response.put("errors", errors);
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    
    // 请求方法不支持异常处理
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, String>> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException ex) {
        logger.warn("Method not supported", ex);
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("message", "不支持此请求方法");
        return new ResponseEntity<>(errorResponse, HttpStatus.METHOD_NOT_ALLOWED);
    }
    
    // 文件上传大小超限异常处理
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex) {
        logger.warn("Upload size exceeded", ex);
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("message", "上传文件大小超过限制");
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    // 安全异常处理
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, Object>> handleSecurityException(SecurityException ex, WebRequest request) {
        logger.warn("Security exception: {}", ex.getMessage(), ex);
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("errorCode", ex.getErrorCode());
        
        HttpStatus status;
        switch (ex.getErrorCode()) {
            case "SEC-0002":
                status = HttpStatus.FORBIDDEN;
                break;
            case "SEC-0003":
                status = HttpStatus.UNAUTHORIZED;
                break;
            case "SEC-0004":
                status = HttpStatus.LOCKED;
                break;
            case "SEC-0005":
                status = HttpStatus.TOO_MANY_REQUESTS;
                break;
            case "SEC-0006":
                status = HttpStatus.BAD_REQUEST;
                break;
            default:
                status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        
        return new ResponseEntity<>(errorResponse, status);
    }
    
    // 静态资源未找到异常处理
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, String>> handleNoResourceFoundException(NoResourceFoundException ex, WebRequest request) {
        logger.debug("Resource not found: {}", ex.getMessage());
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("message", "请求的资源不存在");
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }
} 