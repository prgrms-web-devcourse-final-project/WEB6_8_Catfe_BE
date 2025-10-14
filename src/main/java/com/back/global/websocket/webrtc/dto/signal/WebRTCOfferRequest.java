package com.back.global.websocket.webrtc.dto.signal;

import com.back.global.websocket.webrtc.dto.media.WebRTCMediaType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WebRTCOfferRequest(
        @NotNull Long roomId,
        @NotNull Long targetUserId,
        @NotNull String sdp,
        @NotNull WebRTCMediaType mediaType
) {
}