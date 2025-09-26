package com.back.domain.chat.room.dto;

import java.util.List;

public record RoomChatPageResponse(
        List<RoomChatMessageDto> content,
        PageableDto pageable,
        long totalElements
) {

    // 페이지 정보 DTO
    public record PageableDto(
            int page,
            int size,
            boolean hasNext
    ) {}

    // 페이지 응답 생성
    public static RoomChatPageResponse from(
            org.springframework.data.domain.Page<?> page,
            List<RoomChatMessageDto> convertedContent) {

        return new RoomChatPageResponse(
                convertedContent,
                new PageableDto(
                        page.getNumber(),
                        page.getSize(),
                        page.hasNext()
                ),
                page.getTotalElements()
        );
    }

    // 빈 페이지 응답 생성
    public static RoomChatPageResponse empty(int page, int size) {
        return new RoomChatPageResponse(
                List.of(),
                new PageableDto(page, size, false),
                0L
        );
    }

    // 단일 페이지 응답 생성 (테스트용)
    public static RoomChatPageResponse of(List<RoomChatMessageDto> content,
                                          int page,
                                          int size,
                                          boolean hasNext,
                                          long totalElements) {
        return new RoomChatPageResponse(
                content,
                new PageableDto(page, size, hasNext),
                totalElements
        );
    }
}