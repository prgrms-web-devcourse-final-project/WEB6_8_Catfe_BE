package com.back.domain.notification.event.studyroom;

import com.back.domain.notification.entity.NotificationType;
import lombok.Getter;

@Getter
public abstract class StudyRoomNotificationEvent {
    private final Long actorId;              // 발신자
    private final Long studyRoomId;          // 스터디룸 ID
    private final NotificationType targetType = NotificationType.ROOM;
    private final String title;
    private final String content;

    protected StudyRoomNotificationEvent(Long actorId, Long studyRoomId, String title, String content) {
        this.actorId = actorId;
        this.studyRoomId = studyRoomId;
        this.title = title;
        this.content = content;
    }
}
