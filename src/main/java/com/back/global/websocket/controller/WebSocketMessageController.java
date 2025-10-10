package com.back.global.websocket.controller;

import com.back.global.exception.CustomException;
import java.security.Principal;
import com.back.global.security.user.CustomUserDetails;
import com.back.global.websocket.service.WebSocketSessionManager;
import com.back.global.websocket.util.WebSocketErrorHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketMessageController {

    private final WebSocketSessionManager sessionManager;
    private final WebSocketErrorHelper errorHelper;

    // Heartbeat 처리
    @MessageMapping("/heartbeat")
    public void handleHeartbeat(Principal principal,
                                SimpMessageHeaderAccessor headerAccessor) {
        try {
            // Principal에서 인증된 사용자 정보 추출
            if (principal instanceof Authentication auth && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
                Long userId = userDetails.getUserId();

                sessionManager.updateLastActivity(userId);
                log.debug("Heartbeat 처리 완료 - 사용자: {}", userId);
            } else {
                log.warn("인증되지 않은 Heartbeat 요청: {}", headerAccessor.getSessionId());
                errorHelper.sendUnauthorizedError(headerAccessor.getSessionId());
            }
        } catch (CustomException e) {
            log.error("Heartbeat 처리 실패: {}", e.getMessage());
            errorHelper.sendCustomExceptionToUser(headerAccessor.getSessionId(), e);
        } catch (Exception e) {
            log.error("Heartbeat 처리 중 예상치 못한 오류", e);
            errorHelper.sendGenericErrorToUser(headerAccessor.getSessionId(), e, "Heartbeat 처리 중 오류가 발생했습니다");
        }
    }

    // 사용자 활동 신호 처리
    @MessageMapping("/activity")
    public void handleActivity(Principal principal,
                               SimpMessageHeaderAccessor headerAccessor) {
        try {
            if (principal instanceof Authentication auth && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
                Long userId = userDetails.getUserId();

                sessionManager.updateLastActivity(userId);
                log.debug("사용자 활동 신호 처리 완료 - 사용자: {}", userId);
            } else {
                log.warn("유효하지 않은 활동 신호: userId가 null");
                errorHelper.sendInvalidRequestError(headerAccessor.getSessionId(), "사용자 ID가 필요합니다");
            }
        } catch (CustomException e) {
            log.error("활동 신호 처리 실패: {}", e.getMessage());
            errorHelper.sendCustomExceptionToUser(headerAccessor.getSessionId(), e);
        } catch (Exception e) {
            log.error("활동 신호 처리 중 예상치 못한 오류", e);
            errorHelper.sendGenericErrorToUser(headerAccessor.getSessionId(), e, "활동 신호 처리 중 오류가 발생했습니다");
        }
    }
}