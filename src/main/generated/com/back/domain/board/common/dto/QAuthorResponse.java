package com.back.domain.board.common.dto;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.ConstructorExpression;
import javax.annotation.processing.Generated;

/**
 * com.back.domain.board.common.dto.QAuthorResponse is a Querydsl Projection type for AuthorResponse
 */
@Generated("com.querydsl.codegen.DefaultProjectionSerializer")
public class QAuthorResponse extends ConstructorExpression<AuthorResponse> {

    private static final long serialVersionUID = 1804686246L;

    public QAuthorResponse(com.querydsl.core.types.Expression<Long> id, com.querydsl.core.types.Expression<String> nickname, com.querydsl.core.types.Expression<String> profileImageUrl) {
        super(AuthorResponse.class, new Class<?>[]{long.class, String.class, String.class}, id, nickname, profileImageUrl);
    }

}

