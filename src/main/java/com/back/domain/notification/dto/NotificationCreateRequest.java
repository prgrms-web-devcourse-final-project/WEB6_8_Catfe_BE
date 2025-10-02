package com.back.domain.notification.dto;

public record NotificationCreateRequest(
        String targetType,      // USER, ROOM, SYSTEM
        Long targetId,          // nullable (SYSTEM일 때 null)
        String title,
        String message,
        String notificationType, // STUDY_REMINDER, ROOM_JOIN 등
        String redirectUrl,      // targetUrl
        String scheduleType,     // ONE_TIME (향후 확장용)
        String scheduledAt       // 예약 시간 (향후 확장용)
) {
}