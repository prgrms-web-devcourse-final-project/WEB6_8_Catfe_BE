package com.back.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 이메일 전송 요청을 나타내는 DTO
 *
 * @param email   이메일 주소
 */
public record SendEmailRequest(
        @NotBlank @Email String email
) {
}
