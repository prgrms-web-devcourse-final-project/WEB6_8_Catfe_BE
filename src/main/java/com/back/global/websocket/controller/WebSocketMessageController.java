package com.back.global.websocket.controller;

import com.back.global.exception.CustomException;
import com.back.global.security.user.CustomUserDetails;
import com.back.global.websocket.service.WebSocketSessionManager;
import com.back.global.websocket.util.WebSocketErrorHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketMessageController {

    private final WebSocketSessionManager sessionManager;
    private final WebSocketErrorHelper errorHelper;

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