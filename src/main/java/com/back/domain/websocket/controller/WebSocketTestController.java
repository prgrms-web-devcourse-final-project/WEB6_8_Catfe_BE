package com.back.domain.websocket.controller;

import com.back.global.common.dto.RsData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/websocket")
public class WebSocketTestController { // WebSocket 기능 테스트용 REST 컨트롤러

    // WebSocket 서버 상태 확인
    @GetMapping("/health")
    public ResponseEntity<RsData<Map<String, Object>>> healthCheck() {
        log.info("WebSocket 헬스체크 요청");

        Map<String, Object> data = new HashMap<>();
        data.put("service", "WebSocket");
        data.put("status", "running");
        data.put("timestamp", LocalDateTime.now());
        data.put("endpoints", Map.of(
                "websocket", "/ws",
                "chat", "/app/rooms/{roomId}/chat",
                "join", "/app/rooms/{roomId}/join",
                "leave", "/app/rooms/{roomId}/leave"
        ));

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("WebSocket 서비스가 정상 동작중입니다.", data));
    }

    // WebSocket 연결 정보 제공
    @GetMapping("/info")
    public ResponseEntity<RsData<Map<String, Object>>> getConnectionInfo() {
        log.info("WebSocket 연결 정보 요청");

        Map<String, Object> connectionInfo = new HashMap<>();
        connectionInfo.put("websocketUrl", "/ws");
        connectionInfo.put("sockjsSupport", true);
        connectionInfo.put("stompVersion", "1.2");
        connectionInfo.put("subscribeTopics", Map.of(
                "roomChat", "/topic/rooms/{roomId}/chat",
                "privateMessage", "/user/queue/messages",
                "notifications", "/user/queue/notifications"
        ));
        connectionInfo.put("sendDestinations", Map.of(
                "roomChat", "/app/rooms/{roomId}/chat",
                "joinRoom", "/app/rooms/{roomId}/join",
                "leaveRoom", "/app/rooms/{roomId}/leave"
        ));

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(RsData.success("WebSocket 연결 정보", connectionInfo));
    }
}