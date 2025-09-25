package com.back.global.websocket.controller;

import com.back.global.common.dto.RsData;
import com.back.global.websocket.service.WebSocketSessionManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/ws")
@RequiredArgsConstructor
@Tag(name = "WebSocket REST API", description = "WebSocket 서버 상태 확인 및 실시간 연결 정보 제공 API")
public class WebSocketApiController {

    private final WebSocketSessionManager sessionManager;

    @GetMapping("/health")
    @Operation(summary = "WebSocket 서버 헬스체크", description = "WebSocket 서비스의 현재 상태를 확인합니다.")
    public ResponseEntity<RsData<Map<String, Object>>> healthCheck() {

        Map<String, Object> data = new HashMap<>();
        data.put("service", "WebSocket");
        data.put("status", "running");
        data.put("timestamp", LocalDateTime.now());
        data.put("sessionTTL", "10분 (Heartbeat 방식)");
        data.put("heartbeatInterval", "5분");
        data.put("totalOnlineUsers", sessionManager.getTotalOnlineUserCount());
        data.put("endpoints", Map.of(
                "websocket", "/ws",
                "heartbeat", "/app/heartbeat",
                "activity", "/app/activity",
                "joinRoom", "/app/rooms/{roomId}/join",
                "leaveRoom", "/app/rooms/{roomId}/leave"
        ));

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("WebSocket 서비스가 정상 동작중입니다.", data));
    }

    @GetMapping("/info")
    @Operation(summary = "WebSocket 연결 정보 조회", description = "클라이언트가 WebSocket에 연결하기 위해 필요한 정보를 제공합니다.")
    public ResponseEntity<RsData<Map<String, Object>>> getConnectionInfo() {

        Map<String, Object> connectionInfo = new HashMap<>();
        connectionInfo.put("websocketUrl", "/ws");
        connectionInfo.put("sockjsSupport", true);
        connectionInfo.put("stompVersion", "1.2");
        connectionInfo.put("heartbeatInterval", "5분");
        connectionInfo.put("sessionTTL", "10분");
        connectionInfo.put("description", "RoomController와 협력하여 실시간 온라인 상태 관리");
        connectionInfo.put("subscribeTopics", Map.of(
                "roomChat", "/topic/rooms/{roomId}/chat",
                "privateMessage", "/user/queue/messages",
                "notifications", "/user/queue/notifications"
        ));
        connectionInfo.put("sendDestinations", Map.of(
                "heartbeat", "/app/heartbeat",
                "activity", "/app/activity",
                "roomChat", "/app/rooms/{roomId}/chat"
        ));

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("WebSocket 연결 정보", connectionInfo));
    }
}