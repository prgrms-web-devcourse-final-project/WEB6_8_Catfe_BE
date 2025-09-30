package com.back.domain.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class TokenServiceTest {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private Long userId;

    @BeforeEach
    void setUp() {
        userId = 1L;
        redisTemplate.getConnectionFactory().getConnection().flushAll(); // 테스트 시작 전 Redis 초기화
    }

    @Test
    @DisplayName("토큰 생성 후 조회 성공")
    void createAndValidateToken() {
        // when
        String token = tokenService.createEmailVerificationToken(userId);
        Long foundUserId = tokenService.getUserIdByEmailVerificationToken(token);

        // then
        assertThat(foundUserId).isEqualTo(userId);
    }

    @Test
    @DisplayName("토큰 삭제 후 조회 실패")
    void deleteToken() {
        // given
        String token = tokenService.createEmailVerificationToken(userId);

        // when
        tokenService.deleteEmailVerificationToken(token);
        Long foundUserId = tokenService.getUserIdByEmailVerificationToken(token);

        // then
        assertThat(foundUserId).isNull();
    }

    @Test
    @DisplayName("토큰 만료 시 조회 실패")
    void tokenExpires() throws InterruptedException {
        // given
        String token = tokenService.createEmailVerificationToken(userId);

        // TTL을 테스트용으로 1초로 오버라이드
        redisTemplate.opsForValue().set("email:verify:" + token, userId.toString(), 1, TimeUnit.SECONDS);

        Thread.sleep(1500); // 1.5초 대기

        // when
        Long foundUserId = tokenService.getUserIdByEmailVerificationToken(token);

        // then
        assertThat(foundUserId).isNull();
    }
}
