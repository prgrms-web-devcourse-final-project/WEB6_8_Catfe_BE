package com.back.domain.user.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

// Redis를 활용한 토큰 관리 서비스
@Service
@RequiredArgsConstructor
public class TokenService {
    private final StringRedisTemplate redisTemplate;

    // -------------------- 이메일 인증 토큰 --------------------
    private static final String EMAIL_VERIFICATION_PREFIX = "email:verify:";
    private static final long EMAIL_VERIFICATION_EXPIRATION_MINUTES = 60 * 24; // 24시간

    public String createEmailVerificationToken(Long userId) {
        return createToken(EMAIL_VERIFICATION_PREFIX, userId, EMAIL_VERIFICATION_EXPIRATION_MINUTES);
    }

    public Long getUserIdByEmailVerificationToken(String token) {
        return getUserIdByToken(EMAIL_VERIFICATION_PREFIX, token);
    }

    public void deleteEmailVerificationToken(String token) {
        deleteToken(EMAIL_VERIFICATION_PREFIX, token);
    }

    // -------------------- 비밀번호 재설정 토큰 --------------------
    private static final String PASSWORD_RESET_PREFIX = "password:reset:";
    private static final long PASSWORD_RESET_EXPIRATION_MINUTES = 60; // 1시간

    public String createPasswordResetToken(Long userId) {
        return createToken(PASSWORD_RESET_PREFIX, userId, PASSWORD_RESET_EXPIRATION_MINUTES);
    }

    public Long getUserIdByPasswordResetToken(String token) {
        return getUserIdByToken(PASSWORD_RESET_PREFIX, token);
    }

    public void deletePasswordResetToken(String token) {
        deleteToken(PASSWORD_RESET_PREFIX, token);
    }

    // -------------------- 내부 공통 로직 --------------------
    private String createToken(String prefix, Long userId, long ttlMinutes) {
        String token = UUID.randomUUID().toString();
        String key = prefix + token;
        redisTemplate.opsForValue().set(key, userId.toString(), ttlMinutes, TimeUnit.MINUTES);
        return token;
    }

    private Long getUserIdByToken(String prefix, String token) {
        String key = prefix + token;
        String userId = redisTemplate.opsForValue().get(key);
        return userId != null ? Long.valueOf(userId) : null;
    }

    private void deleteToken(String prefix, String token) {
        String key = prefix + token;
        redisTemplate.delete(key);
    }
}
