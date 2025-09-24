package com.back.global.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import redis.embedded.RedisServer;
import redis.embedded.RedisServerBuilder;

import java.io.IOException;
import java.net.ServerSocket;

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
    private int port;

    @PostConstruct
    public void startRedis() throws IOException {
        port = findAvailablePort();
        redisServer = new RedisServerBuilder()
                .port(port)
                .setting("maxheap 256M")
                .build();
        redisServer.start();

        // 동적으로 찾은 포트를 Spring 환경 변수에 반영
        System.setProperty("spring.data.redis.port", String.valueOf(port));
    }

    @PreDestroy
    public void stopRedis() {
        if (redisServer != null) {
            redisServer.stop();
        }
    }

    // 사용 가능한 포트를 찾는 유틸리티 메서드
    private int findAvailablePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException("No available port found", e);
        }
    }
}
