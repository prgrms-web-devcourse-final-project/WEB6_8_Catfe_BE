package com.back.global.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class RedisConnectionTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:latest")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void registerRedisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void redisShouldStoreAndGetValue() {
        // given
        String key = "test:key";
        String value = "hello";

        // when
        redisTemplate.opsForValue().set(key, value);
        String result = redisTemplate.opsForValue().get(key);

        // then
        assertThat(result).isEqualTo(value);
    }
}