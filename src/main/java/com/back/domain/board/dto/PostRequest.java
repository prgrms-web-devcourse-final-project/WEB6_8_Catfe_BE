package com.back.domain.board.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * 게시글 생성 및 수정을 위한 요청 DTO
 *
 * @param title         게시글 제목
 * @param content       게시글 내용
 * @param categoryIds   카테고리 ID 리스트
 */
public record PostRequest(
        @NotBlank String title,
        @NotBlank String content,
        List<Long> categoryIds
) {}