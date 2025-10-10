package com.back.global.websocket.controller;

import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import com.back.global.websocket.dto.HeartbeatMessage;
import com.back.global.websocket.service.WebSocketSessionManager;
import com.back.global.websocket.util.WebSocketErrorHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocket 메시지 컨트롤러")
class WebSocketMessageControllerTest {

    @Mock
    private WebSocketSessionManager sessionManager;

    @Mock
    private WebSocketErrorHelper errorHelper;

    @InjectMocks
    private WebSocketMessageController controller;

    private SimpMessageHeaderAccessor headerAccessor;
    private Long userId;
    private Long roomId;
    private String sessionId;

    @BeforeEach
    void setUp() {
        userId = 10L;
        roomId = 1L;
        sessionId = "test-session-id";

        headerAccessor = mock(SimpMessageHeaderAccessor.class);
        lenient().when(headerAccessor.getSessionId()).thenReturn(sessionId);
    }

    @Nested
    @DisplayName("Heartbeat 처리")
    class HandleHeartbeatTest {

        @Test
        @DisplayName("정상 - Heartbeat 처리")
        void t1() {
            // given
            HeartbeatMessage message = new HeartbeatMessage(userId);
            doNothing().when(sessionManager).updateLastActivity(userId);

            // when
            controller.handleHeartbeat(message, headerAccessor);

            // then
            verify(sessionManager).updateLastActivity(userId);
            verify(errorHelper, never()).sendInvalidRequestError(anyString(), anyString());
            verify(errorHelper, never()).sendCustomExceptionToUser(anyString(), any());
            verify(errorHelper, never()).sendGenericErrorToUser(anyString(), any(), anyString());
        }

        @Test
        @DisplayName("실패 - userId가 null")
        void t2() {
            // given
            HeartbeatMessage message = new HeartbeatMessage(null);

            // when
            controller.handleHeartbeat(message, headerAccessor);

            // then
            verify(sessionManager, never()).updateLastActivity(any());
            verify(errorHelper).sendInvalidRequestError(sessionId, "사용자 ID가 필요합니다");
        }

        @Test
        @DisplayName("실패 - CustomException 발생")
        void t3() {
            // given
            HeartbeatMessage message = new HeartbeatMessage(userId);
            CustomException exception = new CustomException(ErrorCode.BAD_REQUEST);
            doThrow(exception).when(sessionManager).updateLastActivity(userId);

            // when
            controller.handleHeartbeat(message, headerAccessor);

            // then
            verify(sessionManager).updateLastActivity(userId);
            verify(errorHelper).sendCustomExceptionToUser(sessionId, exception);
        }

        @Test
        @DisplayName("실패 - 일반 Exception 발생")
        void t4() {
            // given
            HeartbeatMessage message = new HeartbeatMessage(userId);
            RuntimeException exception = new RuntimeException("예상치 못한 오류");
            doThrow(exception).when(sessionManager).updateLastActivity(userId);

            // when
            controller.handleHeartbeat(message, headerAccessor);

            // then
            verify(sessionManager).updateLastActivity(userId);
            verify(errorHelper).sendGenericErrorToUser(
                    eq(sessionId),
                    any(Exception.class),
                    eq("Heartbeat 처리 중 오류가 발생했습니다")
            );
        }
    }

    @Nested
    @DisplayName("활동 신호 처리")
    class HandleActivityTest {

        @Test
        @DisplayName("정상 - 활동 신호 처리")
        void t13() {
            // given
            HeartbeatMessage message = new HeartbeatMessage(userId);
            doNothing().when(sessionManager).updateLastActivity(userId);

            // when
            controller.handleActivity(message, headerAccessor);

            // then
            verify(sessionManager).updateLastActivity(userId);
            verify(errorHelper, never()).sendInvalidRequestError(anyString(), anyString());
            verify(errorHelper, never()).sendCustomExceptionToUser(anyString(), any());
            verify(errorHelper, never()).sendGenericErrorToUser(anyString(), any(), anyString());
        }

        @Test
        @DisplayName("실패 - userId가 null")
        void t14() {
            // given
            HeartbeatMessage message = new HeartbeatMessage(null);

            // when
            controller.handleActivity(message, headerAccessor);

            // then
            verify(sessionManager, never()).updateLastActivity(any());
            verify(errorHelper).sendInvalidRequestError(sessionId, "사용자 ID가 필요합니다");
        }

        @Test
        @DisplayName("실패 - CustomException 발생")
        void t15() {
            // given
            HeartbeatMessage message = new HeartbeatMessage(userId);
            CustomException exception = new CustomException(ErrorCode.BAD_REQUEST);
            doThrow(exception).when(sessionManager).updateLastActivity(userId);

            // when
            controller.handleActivity(message, headerAccessor);

            // then
            verify(sessionManager).updateLastActivity(userId);
            verify(errorHelper).sendCustomExceptionToUser(sessionId, exception);
        }

        @Test
        @DisplayName("실패 - 일반 Exception 발생")
        void t16() {
            // given
            HeartbeatMessage message = new HeartbeatMessage(userId);
            RuntimeException exception = new RuntimeException("예상치 못한 오류");
            doThrow(exception).when(sessionManager).updateLastActivity(userId);

            // when
            controller.handleActivity(message, headerAccessor);

            // then
            verify(sessionManager).updateLastActivity(userId);
            verify(errorHelper).sendGenericErrorToUser(
                    eq(sessionId),
                    any(Exception.class),
                    eq("활동 신호 처리 중 오류가 발생했습니다")
            );
        }
    }
}