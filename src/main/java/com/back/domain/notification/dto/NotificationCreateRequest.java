package com.back.domain.notification.dto;

public record NotificationCreateRequest(
        String targetType,
        Long targetId,
        Long actorId,
        String title,
        String message,
        String redirectUrl
) {}