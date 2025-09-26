package com.back.domain.chat.dm.dto;

import java.time.LocalDateTime;

public record PrivateMessageResponse(
        Long messageId,
        Long fromUserId,
        Long toUserId,
        String fromNickname,
        String fromProfileImageUrl,
        String content,
        String messageType,
        AttachmentDto attachment,
        LocalDateTime createdAt
) {

    /**
     * 첨부파일 정보 DTO
     */
    public record AttachmentDto(
            Long id,
            String originalName,
            String url,
            Long size,
            String mimeType
    ) {}

    /**
     * 텍스트 메시지 응답 생성 헬퍼 메서드
     */
    public static PrivateMessageResponse createTextMessage(
            Long messageId,
            Long fromUserId,
            Long toUserId,
            String fromNickname,
            String fromProfileImageUrl,
            String content,
            LocalDateTime createdAt) {

        return new PrivateMessageResponse(
                messageId,
                fromUserId,
                toUserId,
                fromNickname,
                fromProfileImageUrl,
                content,
                "TEXT",
                null, // attachment
                createdAt
        );
    }
}

