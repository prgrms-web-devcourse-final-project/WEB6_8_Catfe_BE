package com.back.global.websocket.dto;

import java.time.LocalDateTime;

public record WebSocketErrorResponse(
    String type,
    ErrorDto error,
    LocalDateTime timestamp
) {
    
    public record ErrorDto(
        String code,
        String message
    ) {}

    // 에러 응답 생성 헬퍼
    public static WebSocketErrorResponse create(String code, String message) {
        return new WebSocketErrorResponse(
            "ERROR",
            new ErrorDto(code, message),
            LocalDateTime.now()
        );
    }
}
