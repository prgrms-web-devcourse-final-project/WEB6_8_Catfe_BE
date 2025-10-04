package com.back.domain.board.dto;

import com.back.domain.board.entity.PostCategory;
import com.querydsl.core.annotations.QueryProjection;

/**
 * 카테고리 응답 DTO
 *
 * @param id    카테고리 ID
 * @param name  카테고리 이름
 */
public record CategoryResponse(
        Long id,
        String name
) {
    @QueryProjection
    public CategoryResponse {}

    public static CategoryResponse from(PostCategory category) {
        return new CategoryResponse(
                category.getId(),
                category.getName()
        );
    }
}