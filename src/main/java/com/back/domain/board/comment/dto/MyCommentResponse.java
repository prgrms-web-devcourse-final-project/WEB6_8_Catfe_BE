package com.back.domain.board.comment.dto;

import com.querydsl.core.annotations.QueryProjection;

import java.time.LocalDateTime;

/**
 * 내 댓글 목록 응답 DTO
 *
 * @param commentId     댓글 ID
 * @param postId        게시글 ID
 * @param postTitle     게시글 제목
 * @param parentId      부모 댓글 ID
 * @param parentContent 부모 댓글 내용 (50자)
 * @param content       댓글 내용
 * @param likeCount     좋아요 수
 * @param createdAt     댓글 생성 일시
 * @param updatedAt     댓글 수정 일시
 */
public record MyCommentResponse(
        Long commentId,
        Long postId,
        String postTitle,
        Long parentId,
        String parentContent,
        String content,
        long likeCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    @QueryProjection
    public MyCommentResponse {}
}