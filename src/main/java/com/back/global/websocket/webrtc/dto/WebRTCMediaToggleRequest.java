package com.back.global.websocket.webrtc.dto;

import jakarta.validation.constraints.NotNull;

public record WebRTCMediaToggleRequest(
        @NotNull WebRTCMediaType mediaType,
        @NotNull Boolean enabled
) {
}