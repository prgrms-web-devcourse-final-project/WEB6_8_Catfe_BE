package com.back.global.websocket.webrtc.dto.media;

import jakarta.validation.constraints.NotNull;

public record WebRTCMediaToggleRequest(
        @NotNull WebRTCMediaType mediaType,
        @NotNull Boolean enabled
) {
}