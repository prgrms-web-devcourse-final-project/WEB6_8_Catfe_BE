package com.back.domain.user.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 사용자 로그인 요청을 나타내는 DTO
 *
 * @param username 사용자의 로그인 id
 * @param password 사용자의 비밀번호
 */
public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password
) {}