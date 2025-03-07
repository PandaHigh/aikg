package com.know.aikg.util;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 安全工具类，用于敏感信息的加密和解密
 */
public class SecurityUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(SecurityUtil.class);
    private static final String AES_ALGORITHM = "AES";
    private static final String SECURE_RANDOM_ALGORITHM = "SHA1PRNG";
    
    // 注意: 在实际生产环境中，密钥应该从安全的地方获取，比如环境变量或配置服务
    private static final String DEFAULT_SECRET_KEY = "aikg_default_secret_key";
    
    /**
     * 使用AES算法加密数据
     * @param data 需要加密的数据
     * @return 加密后的Base64编码的字符串
     */
    public static String encrypt(String data) {
        return encrypt(data, DEFAULT_SECRET_KEY);
    }
    
    /**
     * 使用AES算法加密数据
     * @param data 需要加密的数据
     * @param secretKey 密钥
     * @return 加密后的Base64编码的字符串
     */
    public static String encrypt(String data, String secretKey) {
        if (data == null || data.isEmpty()) {
            return data;
        }
        
        try {
            SecretKeySpec keySpec = generateKey(secretKey);
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            logger.error("加密数据失败", e);
            throw new RuntimeException("加密数据失败", e);
        }
    }
    
    /**
     * 使用AES算法解密数据
     * @param encryptedData 加密的数据（Base64编码）
     * @return 解密后的字符串
     */
    public static String decrypt(String encryptedData) {
        return decrypt(encryptedData, DEFAULT_SECRET_KEY);
    }
    
    /**
     * 使用AES算法解密数据
     * @param encryptedData 加密的数据（Base64编码）
     * @param secretKey 密钥
     * @return 解密后的字符串
     */
    public static String decrypt(String encryptedData, String secretKey) {
        if (encryptedData == null || encryptedData.isEmpty()) {
            return encryptedData;
        }
        
        try {
            SecretKeySpec keySpec = generateKey(secretKey);
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedData);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("解密数据失败", e);
            throw new RuntimeException("解密数据失败", e);
        }
    }
    
    /**
     * 生成安全的随机密码
     * @param length 密码长度
     * @return 随机密码
     */
    public static String generateSecurePassword(int length) {
        if (length < 8) {
            length = 8; // 确保密码至少8位
        }
        
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+";
        SecureRandom random;
        try {
            random = SecureRandom.getInstance(SECURE_RANDOM_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            logger.warn("无法获取SHA1PRNG算法实例，使用默认SecureRandom", e);
            random = new SecureRandom();
        }
        
        StringBuilder password = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(chars.length());
            password.append(chars.charAt(randomIndex));
        }
        
        return password.toString();
    }
    
    /**
     * 从密钥字符串生成AES密钥
     * @param secretKey 密钥字符串
     * @return SecretKeySpec对象
     */
    private static SecretKeySpec generateKey(String secretKey) throws NoSuchAlgorithmException {
        SecureRandom secureRandom = SecureRandom.getInstance(SECURE_RANDOM_ALGORITHM);
        secureRandom.setSeed(secretKey.getBytes(StandardCharsets.UTF_8));
        
        KeyGenerator keyGenerator = KeyGenerator.getInstance(AES_ALGORITHM);
        keyGenerator.init(128, secureRandom);
        SecretKey key = keyGenerator.generateKey();
        
        return new SecretKeySpec(key.getEncoded(), AES_ALGORITHM);
    }
    
    /**
     * 对敏感信息进行脱敏处理
     * @param info 敏感信息
     * @param prefixLength 保留前几位
     * @param suffixLength 保留后几位
     * @return 脱敏后的信息
     */
    public static String maskSensitiveInfo(String info, int prefixLength, int suffixLength) {
        if (info == null || info.isEmpty()) {
            return info;
        }
        
        int length = info.length();
        if (length <= prefixLength + suffixLength) {
            return "*".repeat(length);
        }
        
        String prefix = info.substring(0, prefixLength);
        String suffix = info.substring(length - suffixLength);
        int maskLength = length - prefixLength - suffixLength;
        
        return prefix + "*".repeat(maskLength) + suffix;
    }
    
    /**
     * 对邮箱进行脱敏处理
     * @param email 邮箱地址
     * @return 脱敏后的邮箱
     */
    public static String maskEmail(String email) {
        if (email == null || email.isEmpty() || !email.contains("@")) {
            return email;
        }
        
        int atIndex = email.indexOf('@');
        String name = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        
        if (name.length() <= 2) {
            return "*" + name.substring(1) + domain;
        } else {
            return name.charAt(0) + "*".repeat(name.length() - 2) + name.charAt(name.length() - 1) + domain;
        }
    }
} 