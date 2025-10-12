package com.back.domain.notification.dto;

import com.back.domain.notification.entity.Notification;
import com.back.domain.notification.service.NotificationService;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Set;

/**
 * 알림 목록 응답 DTO
 */
public record NotificationListResponse(
        List<NotificationItemDto> content,
        PageableDto pageable,
        long unreadCount
) {

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

        // 현재 페이지의 알림들에 대해 읽음 처리된 ID 목록을 한 번에 조회
        Set<Long> readNotificationIds = notificationService.getReadNotificationIds(userId, notifications.getContent());

        List<NotificationItemDto> items = notifications.getContent().stream()
                .map(notification -> {
                    // DB 조회가 아닌 메모리에서 읽음 여부를 빠르게 확인
                    boolean isRead = readNotificationIds.contains(notification.getId());
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