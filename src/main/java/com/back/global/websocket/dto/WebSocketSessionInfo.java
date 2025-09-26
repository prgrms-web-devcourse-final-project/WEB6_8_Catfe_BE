package com.back.global.websocket.dto;

import java.time.LocalDateTime;

public record WebSocketSessionInfo(
    Long userId,
    String sessionId,
    LocalDateTime connectedAt,
    LocalDateTime lastActiveAt,
    Long currentRoomId // 현재 참여 중인 방 ID
) {
    
    // 세션 생성 헬퍼 메소드
    public static WebSocketSessionInfo create(Long userId, String sessionId, Long currentRoomId) {
        LocalDateTime now = LocalDateTime.now();
        return new WebSocketSessionInfo(userId, sessionId, now, now, currentRoomId);
    }
    
    // 마지막 활성 시간 업데이트
    public WebSocketSessionInfo updateLastActiveAt() {
        return new WebSocketSessionInfo(userId, sessionId, connectedAt, LocalDateTime.now(), currentRoomId);
    }
    
    // 현재 방 변경
    public WebSocketSessionInfo changeRoom(Long newRoomId) {
        return new WebSocketSessionInfo(userId, sessionId, connectedAt, LocalDateTime.now(), newRoomId);
    }
}
