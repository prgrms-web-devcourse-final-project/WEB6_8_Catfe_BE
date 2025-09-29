package com.back.global.websocket.util;

import com.back.global.exception.CustomException;
import com.back.global.websocket.dto.WebSocketErrorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * WebSocket 에러 처리 헬퍼 클래스
 * WebSocket Controller에서 공통으로 사용하는 에러 처리 로직 제공
 */
@Component
@RequiredArgsConstructor
public class WebSocketErrorHelper {

    private final SimpMessagingTemplate messagingTemplate;

    // 특정 사용자에게 에러 메시지 전송
    public void sendErrorToUser(String sessionId, String errorCode, String errorMessage) {
        WebSocketErrorResponse errorResponse = WebSocketErrorResponse.create(errorCode, errorMessage);
        messagingTemplate.convertAndSendToUser(sessionId, "/queue/errors", errorResponse);
    }

    // CustomException을 WebSocket 에러로 전송
    public void sendCustomExceptionToUser(String sessionId, CustomException exception) {
        String errorCode = switch (exception.getErrorCode()) {
            case CHAT_DELETE_FORBIDDEN -> "WS_016";
            default -> exception.getErrorCode().getCode();
        };

        sendErrorToUser(sessionId, errorCode, exception.getMessage());
    }

    // 일반 Exception을 기본 WebSocket 에러로 전송
    public void sendGenericErrorToUser(String sessionId, Exception exception, String defaultMessage) {
        sendErrorToUser(sessionId, "WS_015", defaultMessage); // WS_INTERNAL_ERROR
    }

    // 인증 실패 에러 전송
    public void sendUnauthorizedError(String sessionId) {
        sendErrorToUser(sessionId, "WS_009", "인증이 필요합니다");
    }

    // 잘못된 요청 에러 전송
    public void sendInvalidRequestError(String sessionId, String message) {
        sendErrorToUser(sessionId, "WS_014", message);
    }
}