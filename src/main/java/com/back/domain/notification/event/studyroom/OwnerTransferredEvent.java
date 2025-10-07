package com.back.domain.notification.event.studyroom;

import lombok.Getter;

@Getter
public class OwnerTransferredEvent extends StudyRoomNotificationEvent {
    private final Long newOwnerId;
    private final String roomName;

    public OwnerTransferredEvent(Long actorId, Long studyRoomId, Long newOwnerId, String roomName) {
        super(
                actorId,
                studyRoomId,
                "방장 위임",
                String.format("%s 스터디룸의 새로운 방장이 되었습니다", roomName)
        );
        this.newOwnerId = newOwnerId;
        this.roomName = roomName;
    }
}