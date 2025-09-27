package com.back.global.websocket.event;

import com.back.global.exception.CustomException;
import com.back.global.security.user.CustomUserDetails;
import com.back.global.websocket.service.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final WebSocketSessionManager sessionManager;

    // WebSocket 연결 이벤트 처리 - 세션 매니저에 사용자 세션 등록
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        try {
            StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

            // 인증된 사용자 정보 추출
            Authentication auth = (Authentication) headerAccessor.getUser();
            if (auth != null && auth.getPrincipal() instanceof CustomUserDetails userDetails) {

                String sessionId = headerAccessor.getSessionId();
                Long userId = userDetails.getUserId();

                // 세션 매니저에 등록 (TTL 10분 자동 설정)
                sessionManager.addSession(userId, sessionId);

                log.info("WebSocket 연결 완료 - 사용자: {} ({}), 세션: {}",
                        userDetails.getUsername(), userId, sessionId);

            } else {
                log.warn("인증되지 않은 WebSocket 연결 시도 - 세션: {}", headerAccessor.getSessionId());
            }

        } catch (CustomException e) {
            log.error("WebSocket 연결 처리 실패: {}", e.getMessage());
        } catch (Exception e) {
            log.error("WebSocket 연결 처리 중 예상치 못한 오류 발생", e);
        }
    }

    // WebSocket 연결 해제 이벤트 처리 - 세션 매니저에서 사용자 세션 제거
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        try {
            StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
            String sessionId = headerAccessor.getSessionId();

            // 세션 매니저에서 제거 (방 퇴장 처리도 자동 수행)
            sessionManager.removeSession(sessionId);

            log.info("WebSocket 연결 해제 완료 - 세션: {}", sessionId);

        } catch (CustomException e) {
            log.error("WebSocket 연결 해제 처리 실패: {}", e.getMessage());
        } catch (Exception e) {
            log.error("WebSocket 연결 해제 처리 중 예상치 못한 오류 발생", e);
        }
    }
}
