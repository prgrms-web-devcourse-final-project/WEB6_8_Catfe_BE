package com.back;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class RedisConnectionTest {

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
