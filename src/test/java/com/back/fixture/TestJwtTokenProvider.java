package com.back.fixture;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * 테스트용 JWT 토큰 생성기
 */
@Component
public class TestJwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    /**
     * 만료된 리프레시 토큰 생성
     *
     * @param userId 사용자 ID
     * @return 만료된 JWT 리프레시 토큰 문자열
     */
    public String createExpiredRefreshToken(Long userId) {
        Date issuedAt = new Date(System.currentTimeMillis() - 2000L); // 2초 전 발급
        Date expiredAt = new Date(System.currentTimeMillis() - 1000L); // 1초 전 만료

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(issuedAt)
                .expiration(expiredAt)
                .signWith(key)
                .compact();
    }
}
