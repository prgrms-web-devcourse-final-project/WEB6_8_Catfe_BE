package com.back.domain.notification.dto;

import com.back.domain.notification.entity.Notification;
import com.back.domain.notification.entity.NotificationType;

import java.time.LocalDateTime;

/**
 * 알림 응답 DTO
 */
public record NotificationResponse(
        Long notificationId,
        String title,
        String message,
        NotificationType notificationType,
        String targetUrl,
        boolean isRead,
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
                false, // 읽음 여부는 NotificationListResponse에서 처리
                notification.getCreatedAt(),
                null   // readAt은 NotificationRead에서 가져와야 함
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
                notification.getCreatedAt(),
                readAt
        );
    }
}