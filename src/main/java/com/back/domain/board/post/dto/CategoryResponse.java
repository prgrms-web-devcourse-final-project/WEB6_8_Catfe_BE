package com.back.domain.board.post.dto;

import com.back.domain.board.post.entity.PostCategory;
import com.back.domain.board.post.enums.CategoryType;
import com.querydsl.core.annotations.QueryProjection;

/**
 * 카테고리 응답 DTO
 *
 * @param id    카테고리 ID
 * @param name  카테고리 이름
 * @param type  카테고리 분류
 */
public record CategoryResponse(
        Long id,
        String name,
        CategoryType type
) {
    @QueryProjection
    public CategoryResponse {}

    public static CategoryResponse from(PostCategory category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getType()
        );
    }
}