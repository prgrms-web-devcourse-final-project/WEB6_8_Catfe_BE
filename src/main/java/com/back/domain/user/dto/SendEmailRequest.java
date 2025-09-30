package com.back.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SendEmailRequest(
        @NotBlank @Email String email
) {
}
