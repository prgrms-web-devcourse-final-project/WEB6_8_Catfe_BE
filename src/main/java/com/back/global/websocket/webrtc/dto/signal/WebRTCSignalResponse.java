package com.back.global.websocket.webrtc.dto.signal;

import com.back.global.websocket.webrtc.dto.media.WebRTCMediaType;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public record WebRTCSignalResponse(
        WebRTCSignalType type,
        Long fromUserId,
        Long targetUserId,
        Long roomId,
        String sdp,
        WebRTCMediaType mediaType,
        String candidate,
        String sdpMid,
        Integer sdpMLineIndex,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp
) {
    // Offer, Answer용 생성자
    public static WebRTCSignalResponse offerOrAnswer(
            WebRTCSignalType type,
            Long fromUserId,
            Long targetUserId,
            Long roomId,
            String sdp,
            WebRTCMediaType mediaType
    ) {
        return new WebRTCSignalResponse(
                type,
                fromUserId,
                targetUserId,
                roomId,
                sdp,
                mediaType,
                null,
                null,
                null,
                LocalDateTime.now()
        );
    }

    // ICE Candidate용 생성자
    public static WebRTCSignalResponse iceCandidate(
            Long fromUserId,
            Long targetUserId,
            Long roomId,
            String candidate,
            String sdpMid,
            Integer sdpMLineIndex
    ) {
        return new WebRTCSignalResponse(
                WebRTCSignalType.ICE_CANDIDATE,
                fromUserId,
                targetUserId,
                roomId,
                null,
                null,
                candidate,
                sdpMid,
                sdpMLineIndex,
                LocalDateTime.now()
        );
    }
}