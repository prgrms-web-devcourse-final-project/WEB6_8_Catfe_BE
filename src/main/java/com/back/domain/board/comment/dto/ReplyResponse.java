package com.back.domain.board.comment.dto;

import com.back.domain.board.comment.entity.Comment;
import com.back.domain.board.common.dto.AuthorResponse;

import java.time.LocalDateTime;

/**
 * 대댓글 응답 DTO
 *
 * @param commentId 댓글 Id
 * @param postId    게시글 Id
 * @param parentId  부모 댓글 Id
 * @param author    작성자 정보
 * @param content   댓글 내용
 * @param createdAt 댓글 생성 일시
 * @param updatedAt 댓글 수정 일시
 */
public record ReplyResponse(
        Long commentId,
        Long postId,
        Long parentId,
        AuthorResponse author,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ReplyResponse from(Comment comment) {
        return new ReplyResponse(
                comment.getId(),
                comment.getPost().getId(),
                comment.getParent().getId(),
                AuthorResponse.from(comment.getUser()),
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
