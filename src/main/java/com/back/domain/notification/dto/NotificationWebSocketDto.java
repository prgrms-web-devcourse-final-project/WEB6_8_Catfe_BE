package com.back.domain.notification.dto;

import com.back.domain.notification.entity.Notification;
import com.back.domain.notification.entity.NotificationType;

import java.time.LocalDateTime;

public record NotificationWebSocketDto(
        Long notificationId,
        String title,
        String message,
        NotificationType notificationType,
        String targetUrl,
        ActorDto actor,
        LocalDateTime createdAt
) {
    public static NotificationWebSocketDto from(Notification notification) {
        return new NotificationWebSocketDto(
                notification.getId(),
                notification.getTitle(),
                notification.getContent(),
                notification.getType(),
                notification.getTargetUrl(),
                ActorDto.from(notification.getActor()),
                notification.getCreatedAt()
        );
    }
}