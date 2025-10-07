package com.back.domain.notification.event.studyroom;

import lombok.Getter;

@Getter
public class MemberKickedEvent extends StudyRoomNotificationEvent {
    private final Long targetUserId;
    private final String roomName;

    public MemberKickedEvent(Long actorId, Long studyRoomId, Long targetUserId, String roomName) {
        super(
                actorId,
                studyRoomId,
                "스터디룸 퇴출",
                String.format("%s 스터디룸에서 퇴출되었습니다", roomName)
        );
        this.targetUserId = targetUserId;
        this.roomName = roomName;
    }
}