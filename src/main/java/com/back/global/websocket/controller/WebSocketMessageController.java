package com.back.global.websocket.controller;

import com.back.global.exception.CustomException;
import com.back.global.websocket.dto.HeartbeatMessage;
import com.back.global.websocket.service.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketMessageController {

    private final WebSocketSessionManager sessionManager;

    // Heartbeat 처리
    @MessageMapping("/heartbeat")
    public void handleHeartbeat(@Payload HeartbeatMessage message) {
        try {
            if (message.getUserId() != null) {
                // TTL 10분으로 연장
                sessionManager.updateLastActivity(message.getUserId());
                log.debug("Heartbeat 처리 완료 - 사용자: {}", message.getUserId());
            } else {
                log.warn("유효하지 않은 Heartbeat 메시지 수신: userId가 null");
            }
        } catch (CustomException e) {
            log.error("Heartbeat 처리 실패: {}", e.getMessage());
            // STOMP에서는 에러 응답을 보내지 않고 로깅만 (연결 유지)
        } catch (Exception e) {
            log.error("Heartbeat 처리 중 예상치 못한 오류", e);
        }
    }

    // 방 입장 처리
    @MessageMapping("/rooms/{roomId}/join")
    public void handleJoinRoom(@DestinationVariable Long roomId, @Payload HeartbeatMessage message) {
        try {
            if (message.getUserId() != null) {
                sessionManager.joinRoom(message.getUserId(), roomId);
                log.info("STOMP 방 입장 처리 완료 - 사용자: {}, 방: {}", message.getUserId(), roomId);
            } else {
                log.warn("유효하지 않은 방 입장 요청: userId가 null");
            }
        } catch (CustomException e) {
            log.error("방 입장 처리 실패 - 방: {}, 에러: {}", roomId, e.getMessage());
        } catch (Exception e) {
            log.error("방 입장 처리 중 예상치 못한 오류 - 방: {}", roomId, e);
        }
    }

    // 방 퇴장 처리
    @MessageMapping("/rooms/{roomId}/leave")
    public void handleLeaveRoom(@DestinationVariable Long roomId, @Payload HeartbeatMessage message) {
        try {
            if (message.getUserId() != null) {
                sessionManager.leaveRoom(message.getUserId(), roomId);
                log.info("STOMP 방 퇴장 처리 완료 - 사용자: {}, 방: {}", message.getUserId(), roomId);
            } else {
                log.warn("유효하지 않은 방 퇴장 요청: userId가 null");
            }
        } catch (CustomException e) {
            log.error("방 퇴장 처리 실패 - 방: {}, 에러: {}", roomId, e.getMessage());
        } catch (Exception e) {
            log.error("방 퇴장 처리 중 예상치 못한 오류 - 방: {}", roomId, e);
        }
    }

    // 활동 신호 처리
    @MessageMapping("/activity")
    public void handleActivity(@Payload HeartbeatMessage message) {
        try {
            if (message.getUserId() != null) {
                sessionManager.updateLastActivity(message.getUserId());
                log.debug("사용자 활동 신호 처리 완료 - 사용자: {}", message.getUserId());
            }
        } catch (CustomException e) {
            log.error("활동 신호 처리 실패: {}", e.getMessage());
        } catch (Exception e) {
            log.error("활동 신호 처리 중 예상치 못한 오류", e);
        }
    }
}