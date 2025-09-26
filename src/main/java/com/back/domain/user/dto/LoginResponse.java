package com.back.domain.user.dto;

public record LoginResponse(
        String accessToken,
        UserResponse user
) {}
