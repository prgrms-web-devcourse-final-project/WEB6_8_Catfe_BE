package com.back.domain.board.comment.dto;

import com.back.domain.board.comment.entity.Comment;

/**
 * 댓글 좋아요 응답 DTO
 *
 * @param commentId 댓글 ID
 * @param likeCount 좋아요 수
 */
public record CommentLikeResponse(
        Long commentId,
        Long likeCount
) {
    public static CommentLikeResponse from(Comment comment) {
        return new CommentLikeResponse(
                comment.getId(),
                comment.getLikeCount()
        );
    }
}
