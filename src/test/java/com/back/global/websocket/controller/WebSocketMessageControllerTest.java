package com.back.global.websocket.controller;

import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import com.back.global.security.user.CustomUserDetails;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.security.Principal;

import static org.junit.jupiter.api.Assertions.assertThrows;
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
    private String sessionId;

    @BeforeEach
    void setUp() {
        userId = 10L;
        sessionId = "test-session-id";

        headerAccessor = mock(SimpMessageHeaderAccessor.class);
        lenient().when(headerAccessor.getSessionId()).thenReturn(sessionId);
    }

    // 테스트용 Mock Principal 객체를 생성하는 헬퍼 메소드
    private Principal createMockPrincipal(Long userId) {
        CustomUserDetails mockUserDetails = mock(CustomUserDetails.class);
        when(mockUserDetails.getUserId()).thenReturn(userId);

        return new UsernamePasswordAuthenticationToken(mockUserDetails, null, null);
    }

    @Nested
    @DisplayName("Heartbeat 처리")
    class HandleHeartbeatTest {

        @Test
        @DisplayName("성공 - 인증된 사용자의 Heartbeat 처리")
        void t1() {
            // given
            Principal mockPrincipal = createMockPrincipal(userId);
            doNothing().when(sessionManager).updateLastActivity(userId);

            // when
            controller.handleHeartbeat(mockPrincipal, headerAccessor);

            // then
            verify(sessionManager).updateLastActivity(userId);
            verifyNoInteractions(errorHelper);
        }

        @Test
        @DisplayName("실패 - 인증 정보가 없는 경우")
        void t2() {
            // given
            Principal principal = null;

            // when
            controller.handleHeartbeat(principal, headerAccessor);

            // then
            verify(sessionManager, never()).updateLastActivity(any());
            verify(errorHelper).sendUnauthorizedError(sessionId);
        }

        @Test
        @DisplayName("실패 - CustomException 발생 시 예외를 그대로 던짐")
        void t3() {
            // given
            Principal mockPrincipal = createMockPrincipal(userId);
            CustomException expectedException = new CustomException(ErrorCode.BAD_REQUEST);
            doThrow(expectedException).when(sessionManager).updateLastActivity(userId);

            // when & then
            assertThrows(CustomException.class, () -> {
                controller.handleHeartbeat(mockPrincipal, headerAccessor);
            });

            verify(sessionManager).updateLastActivity(userId);
            verifyNoInteractions(errorHelper);
        }

        @Test
        @DisplayName("실패 - 일반 Exception 발생 시 예외를 그대로 던짐")
        void t4() {
            // given
            Principal mockPrincipal = createMockPrincipal(userId);
            RuntimeException expectedException = new RuntimeException("예상치 못한 오류");
            doThrow(expectedException).when(sessionManager).updateLastActivity(userId);

            // when & then
            assertThrows(RuntimeException.class, () -> {
                controller.handleHeartbeat(mockPrincipal, headerAccessor);
            });

            verify(sessionManager).updateLastActivity(userId);
            verifyNoInteractions(errorHelper);
        }
    }

    @Nested
    @DisplayName("활동 신호 처리")
    class HandleActivityTest {

        @Test
        @DisplayName("성공 - 인증된 사용자의 활동 신호 처리")
        void t1() {
            // given
            Principal mockPrincipal = createMockPrincipal(userId);
            doNothing().when(sessionManager).updateLastActivity(userId);

            // when
            controller.handleActivity(mockPrincipal, headerAccessor);

            // then
            verify(sessionManager).updateLastActivity(userId);
            verifyNoInteractions(errorHelper);
        }

        @Test
        @DisplayName("실패 - 인증 정보가 없는 경우")
        void t2() {
            // given
            Principal principal = null;

            // when
            controller.handleActivity(principal, headerAccessor);

            // then
            verify(sessionManager, never()).updateLastActivity(any());
            verify(errorHelper).sendInvalidRequestError(sessionId, "사용자 ID가 필요합니다");
        }

        @Test
        @DisplayName("실패 - CustomException 발생 시 예외를 그대로 던짐")
        void t3() {
            // given
            Principal mockPrincipal = createMockPrincipal(userId);
            CustomException expectedException = new CustomException(ErrorCode.BAD_REQUEST);
            doThrow(expectedException).when(sessionManager).updateLastActivity(userId);

            // when & then
            assertThrows(CustomException.class, () -> {
                controller.handleActivity(mockPrincipal, headerAccessor);
            });

            verify(sessionManager).updateLastActivity(userId);
            verifyNoInteractions(errorHelper);
        }

        @Test
        @DisplayName("실패 - 일반 Exception 발생 시 예외를 그대로 던짐")
        void t4() {
            // given
            Principal mockPrincipal = createMockPrincipal(userId);
            RuntimeException expectedException = new RuntimeException("예상치 못한 오류");
            doThrow(expectedException).when(sessionManager).updateLastActivity(userId);

            // when & then
            assertThrows(RuntimeException.class, () -> {
                controller.handleActivity(mockPrincipal, headerAccessor);
            });

            verify(sessionManager).updateLastActivity(userId);
            verifyNoInteractions(errorHelper);
        }
    }
}