package com.back.global.websocket.controller;

import com.back.domain.studyroom.service.AvatarService;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import com.back.global.security.user.CustomUserDetails;
import com.back.global.websocket.service.RoomParticipantService;
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
import java.util.HashMap;
import java.util.Map;

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

    @Mock
    private RoomParticipantService roomParticipantService;

    @Mock
    private AvatarService avatarService;

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
    @DisplayName("WebSocket 방 입장 처리")
    class HandleWebSocketJoinRoomTest {

        @Test
        @DisplayName("성공 - 인증된 사용자의 방 입장 확인")
        void t1() {
            // given
            Long roomId = 1L;
            Long avatarId = 5L;
            Map<String, Object> payload = new HashMap<>();
            Principal mockPrincipal = createMockPrincipal(userId);
            
            // Redis에 등록되지 않은 상태 (초대 코드 입장 등)
            when(roomParticipantService.getCurrentRoomId(userId)).thenReturn(null);
            when(avatarService.loadOrCreateAvatar(roomId, userId)).thenReturn(avatarId);
            doNothing().when(sessionManager).updateLastActivity(userId);
            doNothing().when(roomParticipantService).enterRoom(userId, roomId, avatarId);

            // when
            controller.handleWebSocketJoinRoom(roomId, payload, mockPrincipal);

            // then
            verify(sessionManager).updateLastActivity(userId);
            verify(roomParticipantService).getCurrentRoomId(userId);
            verify(avatarService).loadOrCreateAvatar(roomId, userId);
            verify(roomParticipantService).enterRoom(userId, roomId, avatarId);
        }

        @Test
        @DisplayName("실패 - 인증 정보가 없는 경우 아무 동작도 하지 않음")
        void t2() {
            // given
            Long roomId = 1L;
            Map<String, Object> payload = new HashMap<>();
            Principal principal = null;

            // when
            controller.handleWebSocketJoinRoom(roomId, payload, principal);

            // then
            verify(sessionManager, never()).updateLastActivity(any(Long.class));
            verify(roomParticipantService, never()).getCurrentRoomId(any());
        }

        @Test
        @DisplayName("성공 - 이미 Redis에 등록된 사용자는 중복 등록 안 함")
        void t3() {
            // given
            Long roomId = 1L;
            Map<String, Object> payload = new HashMap<>();
            Principal mockPrincipal = createMockPrincipal(userId);
            
            // Redis에 이미 등록된 상태
            when(roomParticipantService.getCurrentRoomId(userId)).thenReturn(roomId);
            doNothing().when(sessionManager).updateLastActivity(userId);

            // when
            controller.handleWebSocketJoinRoom(roomId, payload, mockPrincipal);

            // then
            verify(sessionManager).updateLastActivity(userId);
            verify(roomParticipantService).getCurrentRoomId(userId);
            verify(avatarService, never()).loadOrCreateAvatar(any(), any());
            verify(roomParticipantService, never()).enterRoom(any(), any(), any());
        }
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
            verify(sessionManager, never()).updateLastActivity(any(Long.class));
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

    @Nested
    @DisplayName("예외 처리")
    class ExceptionHandlerTest {

        @Test
        @DisplayName("CustomException 처리 - ErrorHelper를 통해 에러 전송")
        void t1() {
            // given
            CustomException exception = new CustomException(ErrorCode.ROOM_NOT_FOUND);

            // when
            controller.handleCustomException(exception, headerAccessor);

            // then
            verify(errorHelper).sendCustomExceptionToUser(sessionId, exception);
        }

        @Test
        @DisplayName("일반 Exception 처리 - ErrorHelper를 통해 일반 에러 전송")
        void t2() {
            // given
            Exception exception = new RuntimeException("예상치 못한 오류");

            // when
            controller.handleGeneralException(exception, headerAccessor);

            // then
            verify(errorHelper).sendGenericErrorToUser(
                    eq(sessionId),
                    eq(exception),
                    eq("요청 처리 중 서버 오류가 발생했습니다.")
            );
        }
    }
}