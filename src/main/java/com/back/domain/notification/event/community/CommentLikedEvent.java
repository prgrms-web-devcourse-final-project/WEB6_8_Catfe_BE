package com.back.domain.notification.event.community;

import lombok.Getter;

@Getter
public class CommentLikedEvent extends CommunityNotificationEvent {
    private final Long postId;
    private final Long commentId;
    private final String commentContent;

    public CommentLikedEvent(Long actorId, Long commentAuthorId, Long postId,
                             Long commentId, String commentContent) {
        super(
                actorId,
                commentAuthorId,
                commentId,
                "좋아요",
                "회원님의 댓글을 좋아합니다"
        );
        this.postId = postId;
        this.commentId = commentId;
        this.commentContent = commentContent;
    }
}