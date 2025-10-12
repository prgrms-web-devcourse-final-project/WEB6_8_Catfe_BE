package com.back.global.websocket.webrtc.dto;

import com.back.global.exception.CustomException;
import java.time.LocalDateTime;

public record WebRTCErrorResponse(
        String type,
        ErrorPayload error,
        LocalDateTime timestamp
) {
    public record ErrorPayload(String code, String message) {}

    public static WebRTCErrorResponse from(CustomException e) {
        return new WebRTCErrorResponse(
                "ERROR",
                new ErrorPayload(e.getErrorCode().getCode(), e.getMessage()),
                LocalDateTime.now()
        );
    }
}