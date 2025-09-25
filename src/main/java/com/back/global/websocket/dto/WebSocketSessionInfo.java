package com.back.global.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketSessionInfo {
    private Long userId;
    private String sessionId;
    private LocalDateTime connectedAt;
    private LocalDateTime lastActiveAt;
    private Long currentRoomId; // 현재 참여 중인 방 ID
}