package com.back.domain.chat.dm.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ConversationListResponse(
        List<ConversationDto> conversations
) {

    // 개별 대화 정보 DTO
    public record ConversationDto(
            Long userId,
            String nickname,
            String profileImageUrl,
            LastMessageDto lastMessage,
            Integer unreadCount
    ) {}

    // 마지막 메시지 정보 DTO
    public record LastMessageDto(
            String content,
            LocalDateTime createdAt,
            Boolean isRead
    ) {}
}
