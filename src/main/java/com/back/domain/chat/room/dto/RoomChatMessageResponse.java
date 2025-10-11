package com.back.domain.chat.room.dto;

import com.back.domain.studyroom.entity.RoomChatMessage;

import java.time.LocalDateTime;

public record RoomChatMessageResponse(
        Long messageId,
        Long roomId,
        Long userId,
        String nickname,
        String profileImageUrl,
        String content,
        LocalDateTime createdAt
) {
    public static RoomChatMessageResponse from(RoomChatMessage entity) {
        return new RoomChatMessageResponse(
                entity.getId(),
                entity.getRoom().getId(),
                entity.getUser().getId(),
                entity.getUser().getNickname(),
                entity.getUser().getProfileImageUrl(),
                entity.getContent(),
                entity.getCreatedAt()
        );
    }
}
