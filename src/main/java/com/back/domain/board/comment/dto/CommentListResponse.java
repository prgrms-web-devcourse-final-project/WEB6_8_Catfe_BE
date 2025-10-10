package com.back.domain.board.comment.dto;

import com.back.domain.board.common.dto.AuthorResponse;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 댓글 목록 응답 DTO
 */
@Getter
public class CommentListResponse {
    private final Long commentId;
    private final Long postId;
    private final Long parentId;
    private final AuthorResponse author;
    private final String content;

    @Setter
    private long likeCount;

    @Setter
    private Boolean likedByMe;

    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    @Setter
    private List<CommentListResponse> children;

    @QueryProjection
    public CommentListResponse(Long commentId,
                               Long postId,
                               Long parentId,
                               AuthorResponse author,
                               String content,
                               long likeCount,
                               Boolean likedByMe,
                               LocalDateTime createdAt,
                               LocalDateTime updatedAt,
                               List<CommentListResponse> children) {
        this.commentId = commentId;
        this.postId = postId;
        this.parentId = parentId;
        this.author = author;
        this.content = content;
        this.likedByMe = likedByMe;
        this.likeCount = likeCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.children = children;
    }
}