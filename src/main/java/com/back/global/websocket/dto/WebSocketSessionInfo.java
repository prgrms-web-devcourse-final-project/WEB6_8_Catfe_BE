package com.back.global.websocket.dto;

import java.time.LocalDateTime;

public record WebSocketSessionInfo(
        Long userId,
        String sessionId,
        LocalDateTime connectedAt,
        LocalDateTime lastActiveAt,
        Long currentRoomId
) {

    // 새로운 세션 생성
    public static WebSocketSessionInfo createNewSession(Long userId, String sessionId) {
        LocalDateTime now = LocalDateTime.now();
        return new WebSocketSessionInfo(
                userId,
                sessionId,
                now,    // connectedAt
                now,    // lastActiveAt
                null    // currentRoomId
        );
    }

    // 활동 시간 업데이트
    public WebSocketSessionInfo withUpdatedActivity() {
        return new WebSocketSessionInfo(
                userId,
                sessionId,
                connectedAt,
                LocalDateTime.now(),
                currentRoomId
        );
    }

    // 방 입장 시 정보 업데이트
    public WebSocketSessionInfo withRoomId(Long newRoomId) {
        return new WebSocketSessionInfo(
                userId,
                sessionId,
                connectedAt,
                LocalDateTime.now(),
                newRoomId
        );
    }

    // 방 퇴장 시 정보 업데이트
    public WebSocketSessionInfo withoutRoom() {
        return new WebSocketSessionInfo(
                userId,
                sessionId,
                connectedAt,
                LocalDateTime.now(),
                null
        );
    }

    // 특정 방에 입장해 있는지 확인
    public boolean isInRoom(Long roomId) {
        return currentRoomId != null && currentRoomId.equals(roomId);
    }

    // 어떤 방에도 입장해 있는지 확인
    public boolean isInAnyRoom() {
        return currentRoomId != null;
    }
}