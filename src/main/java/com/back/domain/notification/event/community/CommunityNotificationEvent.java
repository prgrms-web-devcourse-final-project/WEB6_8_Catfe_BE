package com.back.domain.notification.event.community;

import com.back.domain.notification.entity.NotificationType;
import lombok.Getter;

@Getter
public abstract class CommunityNotificationEvent {
    private final Long actorId;     // 행동한 사람 (댓글 작성자, 좋아요 누른 사람)
    private final Long receiverId;  // 알림 받을 사람 (게시글/댓글 작성자)
    private final NotificationType targetType = NotificationType.COMMUNITY;
    private final Long targetId;    // 게시글 ID 또는 댓글 ID
    private final String title;
    private final String content;

    protected CommunityNotificationEvent(Long actorId, Long receiverId, Long targetId,
                                         String title, String content) {
        this.actorId = actorId;
        this.receiverId = receiverId;
        this.targetId = targetId;
        this.title = title;
        this.content = content;
    }
}