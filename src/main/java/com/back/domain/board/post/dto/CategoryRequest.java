package com.back.domain.board.post.dto;

import com.back.domain.board.post.enums.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 카테고리 생성 요청 DTO
 *
 * @param name  카테고리 이름
 * @param type  카테고리 분류
 */
public record CategoryRequest(
        @NotBlank String name,
        @NotNull CategoryType type
) {
}
