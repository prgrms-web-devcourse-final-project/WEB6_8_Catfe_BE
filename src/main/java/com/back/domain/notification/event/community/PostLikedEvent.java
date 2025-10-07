package com.back.domain.notification.event.community;

import lombok.Getter;

@Getter
public class PostLikedEvent extends CommunityNotificationEvent {
    private final Long postId;
    private final String postTitle;

    public PostLikedEvent(Long actorId, Long postAuthorId, Long postId, String postTitle) {
        super(
                actorId,
                postAuthorId,
                postId,
                "좋아요",
                "회원님의 게시글을 좋아합니다"
        );
        this.postId = postId;
        this.postTitle = postTitle;
    }
}