package com.back.global.websocket.service;

import com.back.global.websocket.dto.WebSocketSessionInfo;
import com.back.global.websocket.event.SessionDisconnectedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketSessionManager {

    private final UserSessionService userSessionService;
    private final ApplicationEventPublisher eventPublisher;

    // 사용자 세션 추가 (WebSocket 연결 시 호출)
    public void addSession(Long userId, String username, String sessionId) {
        userSessionService.registerSession(userId, username, sessionId);
    }

    // 세션 제거 (WebSocket 연결 종료 시 호출)
    public void removeSession(String sessionId) {
        Long userId = userSessionService.getUserIdBySessionId(sessionId);

        if (userId != null) {
            // 세션 종료 이벤트 발행
            eventPublisher.publishEvent(new SessionDisconnectedEvent(this, userId));

            // 세션 종료 처리
            userSessionService.terminateSession(sessionId);
        } else {
            log.warn("종료할 세션을 찾을 수 없음 - 세션: {}", sessionId);
        }
    }

    // 사용자 연결 상태 확인
    public boolean isUserConnected(Long userId) {
        return userSessionService.isConnected(userId);
    }

    // 사용자 세션 정보 조회
    public WebSocketSessionInfo getSessionInfo(Long userId) {
        return userSessionService.getSessionInfo(userId);
    }

    // Heartbeat 처리 (활동 시간 업데이트 및 TTL 연장)
    public void updateLastActivity(Long userId) {
        userSessionService.processHeartbeat(userId);
    }

    // 전체 온라인 사용자 수 조회
    public long getTotalOnlineUserCount() {
        return userSessionService.getTotalOnlineUserCount();
    }
}