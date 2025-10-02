package com.back.domain.board.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 페이지 응답 DTO
 *
 * @param items         목록 데이터
 * @param page          현재 페이지 번호
 * @param size          페이지 크기
 * @param totalElements 전체 요소 수
 * @param totalPages    전체 페이지 수
 * @param last          마지막 페이지 여부
 * @param <T>           제네릭 타입
 */
public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
