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
public class WebSocketStatusResponse {
    private boolean isConnected;
    private LocalDateTime connectedAt;
    private String sessionId;
    private Long currentRoomId;
    private LocalDateTime lastActiveAt;
}
