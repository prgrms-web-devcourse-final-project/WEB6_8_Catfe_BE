package com.back.domain.chat.room.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatClearRequest(
        @NotBlank(message = "삭제 확인 메시지는 필수입니다")
        String confirmMessage
) {

    // 확인 메시지 상수
    public static final String REQUIRED_CONFIRM_MESSAGE = "모든 채팅을 삭제하겠습니다";

    // 확인 메시지인지 검증
    public boolean isValidConfirmMessage() {
        return REQUIRED_CONFIRM_MESSAGE.equals(confirmMessage);
    }
}
