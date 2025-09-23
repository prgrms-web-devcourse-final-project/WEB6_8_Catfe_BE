package com.back.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {

    // WebSocket Request
    private String content;
    private String messageType;
    private Long attachmentId;

    // WebSocket Response
    private Long messageId;
    private Long roomId;
    private Long userId;
    private String nickname;
    private String profileImageUrl;
    private AttachmentDto attachment;
    private LocalDateTime createdAt;

    // 첨부파일 DTO (나중에 파일 기능 구현 시 사용)
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttachmentDto {
        private Long id;
        private String originalName;
        private String url;
        private Long size;
        private String mimeType;
    }

    // 텍스트 채팅 요청 생성 헬퍼
    public static ChatMessageDto createRequest(String content, String messageType) {
        return ChatMessageDto.builder()
                .content(content)
                .messageType(messageType)
                .attachmentId(null) // 텍스트 채팅에서는 null
                .build();
    }
}