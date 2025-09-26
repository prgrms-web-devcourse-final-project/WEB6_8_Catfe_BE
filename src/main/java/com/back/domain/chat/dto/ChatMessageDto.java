package com.back.domain.chat.dto;

import java.time.LocalDateTime;

public record ChatMessageDto(
    // WebSocket Request
    String content,
    String messageType,
    Long attachmentId,
    
    // WebSocket Response
    Long messageId,
    Long roomId,
    Long userId,
    String nickname,
    String profileImageUrl,
    AttachmentDto attachment,
    LocalDateTime createdAt
) {
    
    // 첨부파일 DTO (나중에 파일 기능 구현 시 사용)
    public record AttachmentDto(
        Long id,
        String originalName,
        String url,
        Long size,
        String mimeType
    ) {}

    // 텍스트 채팅 요청 생성 헬퍼
    public static ChatMessageDto createRequest(String content, String messageType) {
        return new ChatMessageDto(
            content,
            messageType,
            null, // attachmentId - 텍스트 채팅에서는 null
            null, // messageId
            null, // roomId
            null, // userId
            null, // nickname
            null, // profileImageUrl
            null, // attachment
            null  // createdAt
        );
    }
    
    // 필드 업데이트
    public ChatMessageDto withRoomId(Long roomId) {
        return new ChatMessageDto(content, messageType, attachmentId, messageId, roomId, userId, nickname, profileImageUrl, attachment, createdAt);
    }
    
    public ChatMessageDto withUserId(Long userId) {
        return new ChatMessageDto(content, messageType, attachmentId, messageId, roomId, userId, nickname, profileImageUrl, attachment, createdAt);
    }
    
    public ChatMessageDto withNickname(String nickname) {
        return new ChatMessageDto(content, messageType, attachmentId, messageId, roomId, userId, nickname, profileImageUrl, attachment, createdAt);
    }
    
    // Response용 생성자
    public static ChatMessageDto createResponse(
            Long messageId, Long roomId, Long userId, String nickname, 
            String profileImageUrl, String content, String messageType, 
            AttachmentDto attachment, LocalDateTime createdAt) {
        return new ChatMessageDto(
            content, messageType, null, // attachmentId는 request용이므로 null
            messageId, roomId, userId, nickname, profileImageUrl, attachment, createdAt
        );
    }
}
