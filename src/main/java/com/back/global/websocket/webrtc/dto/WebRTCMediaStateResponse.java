package com.back.global.websocket.webrtc.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public record WebRTCMediaStateResponse(
        Long userId,
        String nickname,
        WebRTCMediaType mediaType,
        Boolean enabled,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp
) {
    public static WebRTCMediaStateResponse of(
            Long userId,
            String nickname,
            WebRTCMediaType mediaType,
            Boolean enabled
    ) {
        return new WebRTCMediaStateResponse(
                userId,
                nickname,
                mediaType,
                enabled,
                LocalDateTime.now()
        );
    }
}
