package com.back.global.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketErrorResponse {

    private String type = "ERROR";
    private ErrorDto error;
    private LocalDateTime timestamp;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorDto {
        private String code;
        private String message;
    }

    // 에러 응답 생성 헬퍼
    public static WebSocketErrorResponse create(String code, String message) {
        return WebSocketErrorResponse.builder()
                .type("ERROR")
                .error(ErrorDto.builder()
                        .code(code)
                        .message(message)
                        .build())
                .timestamp(LocalDateTime.now())
                .build();
    }
}