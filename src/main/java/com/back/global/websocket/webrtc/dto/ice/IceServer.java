package com.back.global.websocket.webrtc.dto.ice;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record IceServer(
        String urls,
        String username,
        String credential
) {
    // STUN 서버 (인증 불필요)
    public static IceServer stun(String url) {
        return new IceServer(url, null, null);
    }

    // TURN 서버 (인증 필요)
    public static IceServer turn(String url, String username, String credential) {
        return new IceServer(url, username, credential);
    }
}