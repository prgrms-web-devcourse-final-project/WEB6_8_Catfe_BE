package com.back.domain.notification.dto;

import com.back.domain.notification.entity.Notification;
import com.back.domain.notification.entity.NotificationType;

import java.time.LocalDateTime;

/**
 * 알림 상세 응답 DTO
 */
public record NotificationResponse(
        Long notificationId,
        String title,
        String message,
        NotificationType notificationType,
        String targetUrl,
        boolean isRead,
        ActorDto actor,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getContent(),
                notification.getType(),
                notification.getTargetUrl(),
                false,
                ActorDto.from(notification.getActor()),
                notification.getCreatedAt(),
                null
        );
    }

    public static NotificationResponse from(Notification notification, boolean isRead, LocalDateTime readAt) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getContent(),
                notification.getType(),
                notification.getTargetUrl(),
                isRead,
                ActorDto.from(notification.getActor()),
                notification.getCreatedAt(),
                readAt
        );
    }
}