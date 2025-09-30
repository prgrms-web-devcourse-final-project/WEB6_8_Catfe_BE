package com.back.global.websocket.dto;

import jakarta.validation.constraints.NotNull;

public record WebRTCOfferRequest(
        @NotNull Long roomId,
        @NotNull Long targetUserId,
        @NotNull String sdp,
        @NotNull WebRTCMediaType mediaType
) {
}