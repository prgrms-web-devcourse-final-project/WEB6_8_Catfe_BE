package com.back.global.websocket.controller;

import com.back.global.exception.CustomException;
import com.back.global.websocket.dto.HeartbeatMessage;
import com.back.global.websocket.service.WebSocketSessionManager;
import com.back.global.websocket.util.WebSocketErrorHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketMessageController {

    private final WebSocketSessionManager sessionManager;
    private final WebSocketErrorHelper errorHelper;

    // Heartbeat 처리
    @MessageMapping("/heartbeat")
    public void handleHeartbeat(@Payload HeartbeatMessage message,
                                SimpMessageHeaderAccessor headerAccessor) {
        try {
            if (message.userId() != null) {
                // TTL 10분으로 연장
                sessionManager.updateLastActivity(message.userId());
                log.debug("Heartbeat 처리 완료 - 사용자: {}", message.userId());
            } else {
                log.warn("유효하지 않은 Heartbeat 메시지 수신: userId가 null");
                errorHelper.sendInvalidRequestError(headerAccessor.getSessionId(), "사용자 ID가 필요합니다");
            }
        } catch (CustomException e) {
            log.error("Heartbeat 처리 실패: {}", e.getMessage());
            errorHelper.sendCustomExceptionToUser(headerAccessor.getSessionId(), e);
        } catch (Exception e) {
            log.error("Heartbeat 처리 중 예상치 못한 오류", e);
            errorHelper.sendGenericErrorToUser(headerAccessor.getSessionId(), e, "Heartbeat 처리 중 오류가 발생했습니다");
        }
    }

    // 방 입장 처리
    @MessageMapping("/rooms/{roomId}/join")
    public void handleJoinRoom(@DestinationVariable Long roomId,
                               @Payload HeartbeatMessage message,
                               SimpMessageHeaderAccessor headerAccessor) {
        try {
            if (message.userId() != null) {
                sessionManager.joinRoom(message.userId(), roomId);
                log.info("STOMP 방 입장 처리 완료 - 사용자: {}, 방: {}", message.userId(), roomId);
            } else {
                log.warn("유효하지 않은 방 입장 요청: userId가 null");
                errorHelper.sendInvalidRequestError(headerAccessor.getSessionId(), "사용자 ID가 필요합니다");
            }
        } catch (CustomException e) {
            log.error("방 입장 처리 실패 - 방: {}, 에러: {}", roomId, e.getMessage());
            errorHelper.sendCustomExceptionToUser(headerAccessor.getSessionId(), e);
        } catch (Exception e) {
            log.error("방 입장 처리 중 예상치 못한 오류 - 방: {}", roomId, e);
            errorHelper.sendGenericErrorToUser(headerAccessor.getSessionId(), e, "방 입장 중 오류가 발생했습니다");
        }
    }

    // 방 퇴장 처리
    @MessageMapping("/rooms/{roomId}/leave")
    public void handleLeaveRoom(@DestinationVariable Long roomId,
                                @Payload HeartbeatMessage message,
                                SimpMessageHeaderAccessor headerAccessor) {
        try {
            if (message.userId() != null) {
                sessionManager.leaveRoom(message.userId(), roomId);
                log.info("STOMP 방 퇴장 처리 완료 - 사용자: {}, 방: {}", message.userId(), roomId);
            } else {
                log.warn("유효하지 않은 방 퇴장 요청: userId가 null");
                errorHelper.sendInvalidRequestError(headerAccessor.getSessionId(), "사용자 ID가 필요합니다");
            }
        } catch (CustomException e) {
            log.error("방 퇴장 처리 실패 - 방: {}, 에러: {}", roomId, e.getMessage());
            errorHelper.sendCustomExceptionToUser(headerAccessor.getSessionId(), e);
        } catch (Exception e) {
            log.error("방 퇴장 처리 중 예상치 못한 오류 - 방: {}", roomId, e);
            errorHelper.sendGenericErrorToUser(headerAccessor.getSessionId(), e, "방 퇴장 중 오류가 발생했습니다");
        }
    }

    // 사용자 활동 신호 처리
    @MessageMapping("/activity")
    public void handleActivity(@Payload HeartbeatMessage message,
                               SimpMessageHeaderAccessor headerAccessor) {
        try {
            if (message.userId() != null) {
                sessionManager.updateLastActivity(message.userId());
                log.debug("사용자 활동 신호 처리 완료 - 사용자: {}", message.userId());
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