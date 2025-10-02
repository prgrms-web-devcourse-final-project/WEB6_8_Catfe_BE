package com.back.domain.notification.dto;

import com.back.domain.notification.entity.Notification;
import com.back.domain.notification.service.NotificationService;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 알림 목록 응답 DTO
 */
public record NotificationListResponse(
        List<NotificationItemDto> content,
        PageableDto pageable,
        long unreadCount
) {

    // 페이지 정보 DTO
    public record PageableDto(
            int page,
            int size,
            boolean hasNext
    ) {}

    public static NotificationListResponse from(
            Page<Notification> notifications,
            Long userId,
            long unreadCount,
            NotificationService notificationService) {

        List<NotificationItemDto> items = notifications.getContent().stream()
                .map(notification -> {
                    boolean isRead = notificationService.isNotificationRead(notification.getId(), userId);
                    return NotificationItemDto.from(notification, isRead);
                })
                .toList();

        return new NotificationListResponse(
                items,
                new PageableDto(
                        notifications.getNumber(),
                        notifications.getSize(),
                        notifications.hasNext()
                ),
                unreadCount
        );
    }
}