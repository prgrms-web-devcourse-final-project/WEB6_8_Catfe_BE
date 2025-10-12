package com.back.domain.board.post.dto;

import com.back.domain.board.post.entity.Post;

/**
 * 게시글 좋아요 응답 DTO
 *
 * @param postId    게시글 ID
 * @param likeCount 좋아요 수
 */
public record PostLikeResponse(
        Long postId,
        Long likeCount
) {
    public static PostLikeResponse from(Post post) {
        return new PostLikeResponse(
                post.getId(),
                post.getLikeCount()
        );
    }
}
