package com.back.domain.board.post.dto;

import com.back.domain.board.post.entity.Post;

/**
 * 게시글 북마크 응답 DTO
 *
 * @param postId        게시글 ID
 * @param bookmarkCount 북마크 수
 */
public record PostBookmarkResponse(
        Long postId,
        Long bookmarkCount
) {
    public static PostBookmarkResponse from(Post post) {
        return new PostBookmarkResponse(
                post.getId(),
                post.getBookmarkCount()
        );
    }
}
