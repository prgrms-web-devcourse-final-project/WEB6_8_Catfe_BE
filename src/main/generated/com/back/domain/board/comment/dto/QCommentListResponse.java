package com.back.domain.board.comment.dto;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.ConstructorExpression;
import javax.annotation.processing.Generated;

/**
 * com.back.domain.board.comment.dto.QCommentListResponse is a Querydsl Projection type for CommentListResponse
 */
@Generated("com.querydsl.codegen.DefaultProjectionSerializer")
public class QCommentListResponse extends ConstructorExpression<CommentListResponse> {

    private static final long serialVersionUID = -885351544L;

    public QCommentListResponse(com.querydsl.core.types.Expression<Long> commentId, com.querydsl.core.types.Expression<Long> postId, com.querydsl.core.types.Expression<Long> parentId, com.querydsl.core.types.Expression<com.back.domain.board.common.dto.AuthorResponse> author, com.querydsl.core.types.Expression<String> content, com.querydsl.core.types.Expression<Long> likeCount, com.querydsl.core.types.Expression<Boolean> likedByMe, com.querydsl.core.types.Expression<java.time.LocalDateTime> createdAt, com.querydsl.core.types.Expression<java.time.LocalDateTime> updatedAt, com.querydsl.core.types.Expression<? extends java.util.List<CommentListResponse>> children) {
        super(CommentListResponse.class, new Class<?>[]{long.class, long.class, long.class, com.back.domain.board.common.dto.AuthorResponse.class, String.class, long.class, boolean.class, java.time.LocalDateTime.class, java.time.LocalDateTime.class, java.util.List.class}, commentId, postId, parentId, author, content, likeCount, likedByMe, createdAt, updatedAt, children);
    }

}

