package com.back.domain.board.comment.dto;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.ConstructorExpression;
import javax.annotation.processing.Generated;

/**
 * com.back.domain.board.comment.dto.QMyCommentResponse is a Querydsl Projection type for MyCommentResponse
 */
@Generated("com.querydsl.codegen.DefaultProjectionSerializer")
public class QMyCommentResponse extends ConstructorExpression<MyCommentResponse> {

    private static final long serialVersionUID = -688876674L;

    public QMyCommentResponse(com.querydsl.core.types.Expression<Long> commentId, com.querydsl.core.types.Expression<Long> postId, com.querydsl.core.types.Expression<String> postTitle, com.querydsl.core.types.Expression<Long> parentId, com.querydsl.core.types.Expression<String> parentContent, com.querydsl.core.types.Expression<String> content, com.querydsl.core.types.Expression<Long> likeCount, com.querydsl.core.types.Expression<java.time.LocalDateTime> createdAt, com.querydsl.core.types.Expression<java.time.LocalDateTime> updatedAt) {
        super(MyCommentResponse.class, new Class<?>[]{long.class, long.class, String.class, long.class, String.class, String.class, long.class, java.time.LocalDateTime.class, java.time.LocalDateTime.class}, commentId, postId, postTitle, parentId, parentContent, content, likeCount, createdAt, updatedAt);
    }

}

