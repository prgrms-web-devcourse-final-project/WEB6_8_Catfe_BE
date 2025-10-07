package com.back.domain.notification.event.studyroom;

import lombok.Getter;

@Getter
public class MemberRoleChangedEvent extends StudyRoomNotificationEvent {
    private final Long targetUserId;
    private final String newRole;

    public MemberRoleChangedEvent(Long actorId, Long studyRoomId, Long targetUserId, String newRole) {
        super(
                actorId,
                studyRoomId,
                "권한 변경",
                String.format("회원님의 권한이 %s(으)로 변경되었습니다", newRole)
        );
        this.targetUserId = targetUserId;
        this.newRole = newRole;
    }
}