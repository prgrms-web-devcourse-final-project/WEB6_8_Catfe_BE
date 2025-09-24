package com.back.global.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import redis.embedded.RedisServer;
import redis.embedded.RedisServerBuilder;

import java.io.IOException;

/**
 * Embedded Redis 설정 클래스
 * - 개발 및 테스트 환경에서만 활성화 (dev, test 프로파일)
 * - 애플리케이션 시작 시 내장 Redis 서버 시작
 * - 애플리케이션 종료 시 내장 Redis 서버 중지
 */
@Configuration
@Profile({"dev", "test"})
public class EmbeddedRedisConfig {

    private RedisServer redisServer;

    public EmbeddedRedisConfig() throws IOException {
        this.redisServer = new RedisServerBuilder()
                .port(6379)
                .setting("maxheap 256M")
                .build();
    }

    @PostConstruct
    public void startRedis() {
        redisServer.start();
    }

    @PreDestroy
    public void stopRedis() {
        if (redisServer != null) {
            redisServer.stop();
        }
    }
}
