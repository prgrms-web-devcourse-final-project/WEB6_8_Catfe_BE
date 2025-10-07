package com.back.domain.notification.event.community;

import lombok.Getter;

@Getter
public class CommentCreatedEvent extends CommunityNotificationEvent {
    private final Long postId;
    private final Long commentId;
    private final String commentContent;

    public CommentCreatedEvent(Long actorId, Long postAuthorId, Long postId,
                               Long commentId, String commentContent) {
        super(
                actorId,
                postAuthorId,
                postId,
                "새 댓글",
                "회원님의 게시글에 댓글이 달렸습니다"
        );
        this.postId = postId;
        this.commentId = commentId;
        this.commentContent = commentContent;
    }

    public String getContentPreview() {
        if (commentContent == null || commentContent.isEmpty()) {
            return "";
        }
        return commentContent.length() > 50
                ? commentContent.substring(0, 50) + "..."
                : commentContent;
    }
}