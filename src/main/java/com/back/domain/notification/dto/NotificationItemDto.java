package com.back.domain.notification.dto;

import com.back.domain.notification.entity.Notification;
import com.back.domain.notification.entity.NotificationType;

import java.time.LocalDateTime;

/**
 * 알림 목록 아이템 DTO
 */
public record NotificationItemDto(
        Long notificationId,
        String title,
        String message,
        NotificationType notificationType,
        String targetUrl,
        boolean isRead,
        LocalDateTime createdAt
) {
    public static NotificationItemDto from(Notification notification, boolean isRead) {
        return new NotificationItemDto(
                notification.getId(),
                notification.getTitle(),
                notification.getContent(),
                notification.getType(),
                notification.getTargetUrl(),
                isRead,
                notification.getCreatedAt()
        );
    }
}