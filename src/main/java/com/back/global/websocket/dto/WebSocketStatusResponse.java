package com.back.global.websocket.dto;

import java.time.LocalDateTime;

public record WebSocketStatusResponse(
    boolean isConnected,
    LocalDateTime connectedAt,
    String sessionId,
    Long currentRoomId,
    LocalDateTime lastActiveAt
) {
    
    // 연결된 상태 응답 생성
    public static WebSocketStatusResponse connected(String sessionId, Long currentRoomId, LocalDateTime connectedAt, LocalDateTime lastActiveAt) {
        return new WebSocketStatusResponse(true, connectedAt, sessionId, currentRoomId, lastActiveAt);
    }
    
    // 연결 끊긴 상태 응답 생성
    public static WebSocketStatusResponse disconnected() {
        return new WebSocketStatusResponse(false, null, null, null, null);
    }
}
