package com.back.domain.notification.event.study;

import lombok.Getter;

@Getter
public class StudyRecordCreatedEvent extends StudyNotificationEvent {
    private final Long studyRecordId;
    private final Long studyPlanId;
    private final Long duration; // 초 단위

    public StudyRecordCreatedEvent(Long userId, Long studyRecordId, Long studyPlanId, Long duration) {
        super(
                userId,
                "학습 기록 등록",
                formatDuration(duration) + " 공부하셨습니다! 수고하셨어요 🎉"
        );
        this.studyRecordId = studyRecordId;
        this.studyPlanId = studyPlanId;
        this.duration = duration;
    }

    private static String formatDuration(Long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;

        if (hours > 0) {
            return hours + "시간 " + minutes + "분";
        }
        return minutes + "분";
    }
}