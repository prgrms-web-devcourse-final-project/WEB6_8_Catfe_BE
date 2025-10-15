package com.back.domain.board.post.dto;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.ConstructorExpression;
import javax.annotation.processing.Generated;

/**
 * com.back.domain.board.post.dto.QPostListResponse is a Querydsl Projection type for PostListResponse
 */
@Generated("com.querydsl.codegen.DefaultProjectionSerializer")
public class QPostListResponse extends ConstructorExpression<PostListResponse> {

    private static final long serialVersionUID = -138220444L;

    public QPostListResponse(com.querydsl.core.types.Expression<Long> postId, com.querydsl.core.types.Expression<com.back.domain.board.common.dto.AuthorResponse> author, com.querydsl.core.types.Expression<String> title, com.querydsl.core.types.Expression<String> thumbnailUrl, com.querydsl.core.types.Expression<? extends java.util.List<CategoryResponse>> categories, com.querydsl.core.types.Expression<Long> likeCount, com.querydsl.core.types.Expression<Long> bookmarkCount, com.querydsl.core.types.Expression<Long> commentCount, com.querydsl.core.types.Expression<java.time.LocalDateTime> createdAt, com.querydsl.core.types.Expression<java.time.LocalDateTime> updatedAt) {
        super(PostListResponse.class, new Class<?>[]{long.class, com.back.domain.board.common.dto.AuthorResponse.class, String.class, String.class, java.util.List.class, long.class, long.class, long.class, java.time.LocalDateTime.class, java.time.LocalDateTime.class}, postId, author, title, thumbnailUrl, categories, likeCount, bookmarkCount, commentCount, createdAt, updatedAt);
    }

}

