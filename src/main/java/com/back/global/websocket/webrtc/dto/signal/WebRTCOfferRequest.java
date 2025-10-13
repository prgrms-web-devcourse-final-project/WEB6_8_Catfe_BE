package com.back.global.websocket.webrtc.dto.signal;

import com.back.global.websocket.webrtc.dto.media.WebRTCMediaType;
import jakarta.validation.constraints.NotNull;

public record WebRTCOfferRequest(
        @NotNull Long roomId,
        @NotNull Long targetUserId,
        @NotNull SdpData sdp,
        @NotNull WebRTCMediaType mediaType
) {
}