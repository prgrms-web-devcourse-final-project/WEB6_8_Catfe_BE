package com.back.domain.chat.dm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PrivateMessageRequest(
        @NotNull(message = "수신자 ID는 필수입니다")
        Long toUserId,

        @NotBlank(message = "메시지 내용은 필수입니다")
        String content,

        String messageType,  // TEXT, IMAGE, FILE 등 (기본값: TEXT)

        Long attachmentId    // 첨부파일 ID (선택사항)
) {

    // 기본값 처리를 위한 정적 팩토리 메서드
    public static PrivateMessageRequest of(Long toUserId, String content) {
        return new PrivateMessageRequest(toUserId, content, "TEXT", null);
    }

    // messageType 기본값 처리
    public String messageType() {
        return messageType != null ? messageType : "TEXT";
    }
}
