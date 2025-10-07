package com.back.domain.notification.event.study;

import com.back.domain.notification.entity.NotificationType;
import lombok.Getter;

@Getter
public abstract class StudyNotificationEvent {
    private final Long userId;  // 알림 받을 사용자 (본인)
    private final NotificationType targetType = NotificationType.PERSONAL;
    private final String title;
    private final String content;

    protected StudyNotificationEvent(Long userId, String title, String content) {
        this.userId = userId;
        this.title = title;
        this.content = content;
    }
}