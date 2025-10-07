package com.back.domain.notification.event.studyroom;

import lombok.Getter;

@Getter
public class StudyRoomNoticeCreatedEvent extends StudyRoomNotificationEvent {
    private final String noticeTitle;
    private final String noticeContent;

    public StudyRoomNoticeCreatedEvent(Long actorId, Long studyRoomId, String noticeTitle, String noticeContent) {
        super(
                actorId,
                studyRoomId,
                "새로운 공지사항",
                "새로운 공지사항이 등록되었습니다"
        );
        this.noticeTitle = noticeTitle;
        this.noticeContent = noticeContent;
    }

    public String getContentPreview() {
        return noticeContent != null && noticeContent.length() > 50
                ? noticeContent.substring(0, 50) + "..."
                : noticeContent;
    }
}