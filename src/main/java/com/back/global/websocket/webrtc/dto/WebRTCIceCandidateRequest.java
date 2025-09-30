package com.back.global.websocket.webrtc.dto;

import jakarta.validation.constraints.NotNull;

public record WebRTCIceCandidateRequest(
        @NotNull Long roomId,
        @NotNull Long targetUserId,
        @NotNull String candidate,
        @NotNull String sdpMid,
        @NotNull Integer sdpMLineIndex
) {
}