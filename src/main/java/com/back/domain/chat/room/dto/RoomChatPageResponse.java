package com.back.domain.chat.room.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomChatPageResponse {

    private List<RoomChatMessageDto> content;
    private PageableDto pageable;
    private long totalElements;

    // 페이징 정보 DTO
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageableDto {
        private int page;
        private int size;
        private boolean hasNext;
    }

    // Page<RoomChatMessageDto> -> RoomChatPageResponse 변환 헬퍼
    public static RoomChatPageResponse from(Page<RoomChatMessageDto> page) {
        return RoomChatPageResponse.builder()
                .content(page.getContent())
                .pageable(PageableDto.builder()
                        .page(page.getNumber())
                        .size(page.getSize())
                        .hasNext(page.hasNext())
                        .build())
                .totalElements(page.getTotalElements())
                .build();
    }
}