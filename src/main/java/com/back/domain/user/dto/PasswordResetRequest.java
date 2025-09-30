package com.back.domain.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 비밀번호 재설정 요청 DTO
 *
 * @param token         비밀번호 재설정 토큰
 * @param newPassword   새 비밀번호
 */
public record PasswordResetRequest(
        @NotBlank String token,
        @NotBlank String newPassword
) {
}
