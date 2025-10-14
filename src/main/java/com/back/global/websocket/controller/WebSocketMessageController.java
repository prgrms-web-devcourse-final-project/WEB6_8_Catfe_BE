package com.back.global.websocket.controller;

import com.back.global.exception.CustomException;
import com.back.global.security.user.CustomUserDetails;
import com.back.global.websocket.service.WebSocketSessionManager;
import com.back.global.websocket.util.WebSocketAuthHelper;
import com.back.global.websocket.util.WebSocketErrorHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketMessageController {

    private final WebSocketSessionManager sessionManager;
    private final WebSocketErrorHelper errorHelper;

    // WebSocket 방 입장 확인 메시지
    // 클라이언트가 REST API로 입장 후 WebSocket 세션 동기화 대기를 위해 전송
    @MessageMapping("/rooms/{roomId}/join")
    public void handleWebSocketJoinRoom(@DestinationVariable Long roomId,
                                        @Payload Map<String, Object> payload,
                                        Principal principal) {
        CustomUserDetails userDetails = WebSocketAuthHelper.extractUserDetails(principal);
        if (userDetails == null) {
            log.warn("📥 [WebSocket] 방 입장 실패 - 인증 정보 없음");
            return;
        }

        Long userId = userDetails.getUserId();
        log.info("📥 [WebSocket] 방 입장 확인 - roomId: {}, userId: {}", roomId, userId);

        // 활동 시간 업데이트
        sessionManager.updateLastActivity(userId);

        // 실제 방 입장 로직은 REST API에서 이미 처리했으므로
        // 여기서는 단순히 WebSocket 세션이 준비되었음을 확인하는 용도
    }

    // Heartbeat 처리
    @MessageMapping("/heartbeat")
    public void handleHeartbeat(Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        if (principal instanceof Authentication auth && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            Long userId = userDetails.getUserId();
            sessionManager.updateLastActivity(userId);
            log.debug("Heartbeat 처리 완료 - 사용자: {}", userId);
        } else {
            log.warn("인증되지 않은 Heartbeat 요청: {}", headerAccessor.getSessionId());
            errorHelper.sendUnauthorizedError(headerAccessor.getSessionId());
        }
    }

    // 사용자 활동 신호 처리
    @MessageMapping("/activity")
    public void handleActivity(Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        if (principal instanceof Authentication auth && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            Long userId = userDetails.getUserId();
            sessionManager.updateLastActivity(userId);
            log.debug("사용자 활동 신호 처리 완료 - 사용자: {}", userId);
        } else {
            log.warn("유효하지 않은 활동 신호: 인증 정보 없음");
            errorHelper.sendInvalidRequestError(headerAccessor.getSessionId(), "사용자 ID가 필요합니다");
        }
    }

    // WebSocket 메시지 처리 중 발생하는 CustomException 처리
    @MessageExceptionHandler(CustomException.class)
    public void handleCustomException(CustomException e, SimpMessageHeaderAccessor headerAccessor) {
        log.error("WebSocket 처리 중 CustomException 발생: {}", e.getMessage());
        errorHelper.sendCustomExceptionToUser(headerAccessor.getSessionId(), e);
    }

    // 예상치 못한 모든 Exception 처리
    @MessageExceptionHandler(Exception.class)
    public void handleGeneralException(Exception e, SimpMessageHeaderAccessor headerAccessor) {
        log.error("WebSocket 처리 중 예상치 못한 오류 발생", e);
        errorHelper.sendGenericErrorToUser(headerAccessor.getSessionId(), e, "요청 처리 중 서버 오류가 발생했습니다.");
    }
}