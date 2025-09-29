package com.back.domain.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 비밀번호 변경 요청을 나타내는 DTO
 *
 * @param currentPassword 현재 비밀번호
 * @param newPassword 새로운 비밀번호
 */
public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank String newPassword
) {
}
