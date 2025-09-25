package com.back.global.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import redis.embedded.RedisServer;

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
    public void startRedis() {
        try {
            String osName = System.getProperty("os.name").toLowerCase();

            // Mac 환경이면 embedded-redis 건너뛰고 docker-compose Redis 사용
            if (osName.contains("mac")) {
                System.out.println("Mac 환경 감지 → embedded-redis 비활성화, docker-compose Redis 사용");
                System.setProperty("spring.data.redis.port", "6379"); // docker-compose 기본 포트
                return;
            }

            this.port = findAvailablePort();
            this.redisServer = new RedisServer(port);
            this.redisServer.start();
            System.setProperty("spring.data.redis.port", String.valueOf(port));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to start embedded Redis", e);
        }
    }

    @PreDestroy
    public void stopRedis() {
        try {
            if (redisServer != null && redisServer.isActive()) {
                redisServer.stop();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to stop embedded Redis", e);
        }
    }

    private int findAvailablePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException("No available port found", e);
        }
    }
}
