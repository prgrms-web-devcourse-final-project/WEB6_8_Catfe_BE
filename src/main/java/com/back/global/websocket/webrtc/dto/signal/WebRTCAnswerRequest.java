package com.back.global.websocket.webrtc.dto.signal;

import com.back.global.websocket.webrtc.dto.media.WebRTCMediaType;
import jakarta.validation.constraints.NotNull;

public record WebRTCAnswerRequest(
        @NotNull Long roomId,
        @NotNull Long targetUserId,
        @NotNull String sdp,
        @NotNull WebRTCMediaType mediaType
) {
}