package com.back.global.websocket.webrtc.dto.ice;

import java.util.List;

public record IceServerConfig(
        List<IceServer> iceServers
) {
    public static IceServerConfig withDefaultStunServers() {
        return new IceServerConfig(
                List.of(
                        IceServer.stun("stun:stun.l.google.com:19302"),
                        IceServer.stun("stun:stun1.l.google.com:19302"),
                        IceServer.stun("stun:stun2.l.google.com:19302")
                )
        );
    }
}