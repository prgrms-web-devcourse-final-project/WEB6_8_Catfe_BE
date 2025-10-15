package com.back.domain.board.post.dto;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.ConstructorExpression;
import javax.annotation.processing.Generated;

/**
 * com.back.domain.board.post.dto.QCategoryResponse is a Querydsl Projection type for CategoryResponse
 */
@Generated("com.querydsl.codegen.DefaultProjectionSerializer")
public class QCategoryResponse extends ConstructorExpression<CategoryResponse> {

    private static final long serialVersionUID = -1078944764L;

    public QCategoryResponse(com.querydsl.core.types.Expression<Long> id, com.querydsl.core.types.Expression<String> name, com.querydsl.core.types.Expression<com.back.domain.board.post.enums.CategoryType> type) {
        super(CategoryResponse.class, new Class<?>[]{long.class, String.class, com.back.domain.board.post.enums.CategoryType.class}, id, name, type);
    }

}

