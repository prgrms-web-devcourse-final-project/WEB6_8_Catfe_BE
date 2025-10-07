package com.back.domain.notification.event.community;

import lombok.Getter;

@Getter
public class ReplyCreatedEvent extends CommunityNotificationEvent {
    private final Long postId;
    private final Long parentCommentId;
    private final Long replyId;
    private final String replyContent;

    public ReplyCreatedEvent(Long actorId, Long commentAuthorId, Long postId,
                             Long parentCommentId, Long replyId, String replyContent) {
        super(
                actorId,
                commentAuthorId,
                parentCommentId,
                "새 대댓글",
                "회원님의 댓글에 답글이 달렸습니다"
        );
        this.postId = postId;
        this.parentCommentId = parentCommentId;
        this.replyId = replyId;
        this.replyContent = replyContent;
    }

    public String getContentPreview() {
        if (replyContent == null || replyContent.isEmpty()) {
            return "";
        }
        return replyContent.length() > 50
                ? replyContent.substring(0, 50) + "..."
                : replyContent;
    }
}