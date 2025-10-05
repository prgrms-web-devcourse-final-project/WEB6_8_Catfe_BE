package com.back.domain.board.comment.dto;

import com.back.domain.board.common.dto.AuthorResponse;
import com.back.domain.board.comment.entity.Comment;

import java.time.LocalDateTime;

/**
 * 댓글 응답 DTO
 *
 * @param commentId 댓글 Id
 * @param postId    게시글 Id
 * @param author    작성자 정보
 * @param content   댓글 내용
 * @param createdAt 댓글 생성 일시
 * @param updatedAt 댓글 수정 일시
 */
public record CommentResponse(
        Long commentId,
        Long postId,
        AuthorResponse author,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getPost().getId(),
                AuthorResponse.from(comment.getUser()),
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
