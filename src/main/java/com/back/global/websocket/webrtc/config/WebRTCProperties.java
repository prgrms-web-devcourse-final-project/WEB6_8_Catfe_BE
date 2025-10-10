package com.back.global.websocket.webrtc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

@ConfigurationProperties(prefix = "webrtc")
public record WebRTCProperties(
        List<IceServer> iceServers
) {
    // yml의 각 항목과 매핑될 내부 record
    public record IceServer(String urls, String username, String credential) {}
}
