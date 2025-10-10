package com.back.global.websocket.webrtc.dto.ice;

import java.util.List;

public record IceServerConfig(
        List<IceServer> iceServers
) {
}