package com.back.domain.notification.dto;

import com.back.domain.notification.entity.NotificationType;

import java.time.LocalDateTime;

public record NotificationWebSocketDto(
        Long notificationId,
        String title,
        String message,
        NotificationType notificationType,
        String targetUrl,
        LocalDateTime createdAt
) {

    public static NotificationWebSocketDto from(
            Long notificationId,
            String title,
            String content,
            NotificationType type,
            String targetUrl,
            LocalDateTime createdAt) {

        return new NotificationWebSocketDto(
                notificationId,
                title,
                content,
                type,
                targetUrl,
                createdAt
        );
    }
}