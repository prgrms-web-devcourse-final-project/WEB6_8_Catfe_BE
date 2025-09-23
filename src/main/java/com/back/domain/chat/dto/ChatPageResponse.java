package com.back.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatPageResponse {

    private List<ChatMessageDto> content;
    private PageableDto pageable;

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

    // Page<ChatMessageDto> -> ChatPageResponse 변환 헬퍼
    public static ChatPageResponse from(org.springframework.data.domain.Page<ChatMessageDto> page) {
        return ChatPageResponse.builder()
                .content(page.getContent())
                .pageable(PageableDto.builder()
                        .page(page.getNumber())
                        .size(page.getSize())
                        .hasNext(page.hasNext())
                        .build())
                .build();
    }
}