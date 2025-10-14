package com.back.domain.user.auth.dto;

/**
 * 사용자 로그인 응답을 나타내는 DTO
 *
 * @param accessToken   사용자 인증에 사용되는 JWT 토큰
 * @param user          로그인한 사용자 정보
 */
public record LoginResponse(
        String accessToken,
        UserResponse user
) {}
