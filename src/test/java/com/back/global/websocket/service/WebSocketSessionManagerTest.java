package com.back.global.websocket.service;

import com.back.global.websocket.dto.WebSocketSessionInfo;
import com.back.global.websocket.event.SessionDisconnectedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocketSessionManager 단위 테스트")
class WebSocketSessionManagerTest {

    @Mock
    private UserSessionService userSessionService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private WebSocketSessionManager sessionManager;

    private Long userId;
    private String username;
    private String sessionId;

    @BeforeEach
    void setUp() {
        userId = 1L;
        username = "testuser";
        sessionId = "test-session-123";
    }

    @Test
    @DisplayName("세션 추가 - UserSessionService의 registerSession 호출")
    void addSession_CallsRegisterSession() {
        // when
        sessionManager.addSession(userId, username, sessionId);

        // then
        verify(userSessionService).registerSession(userId, username, sessionId);
    }

    @Test
    @DisplayName("세션 제거 - 정상 케이스, SessionDisconnectedEvent 발행")
    void removeSession_Success_PublishesEvent() {
        // given
        given(userSessionService.getUserIdBySessionId(sessionId)).willReturn(userId);
        ArgumentCaptor<SessionDisconnectedEvent> eventCaptor = ArgumentCaptor.forClass(SessionDisconnectedEvent.class);

        // when
        sessionManager.removeSession(sessionId);

        // then
        // 1. 이벤트 발행 검증
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        SessionDisconnectedEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.getUserId()).isEqualTo(userId);

        // 2. 세션 종료 처리 검증
        verify(userSessionService).terminateSession(sessionId);
    }

    @Test
    @DisplayName("세션 제거 - 존재하지 않는 세션, 이벤트 발행 안함")
    void removeSession_NotFound_DoesNotPublishEvent() {
        // given
        given(userSessionService.getUserIdBySessionId(sessionId)).willReturn(null);

        // when
        sessionManager.removeSession(sessionId);

        // then
        verify(userSessionService).getUserIdBySessionId(sessionId);
        // 이벤트가 발행되지 않았는지 검증
        verify(eventPublisher, never()).publishEvent(any());
        verify(userSessionService, never()).terminateSession(anyString());
    }

    @Test
    @DisplayName("사용자 연결 상태 확인")
    void isUserConnected() {
        // given
        given(userSessionService.isConnected(userId)).willReturn(true);

        // when
        boolean result = sessionManager.isUserConnected(userId);

        // then
        assertThat(result).isTrue();
        verify(userSessionService).isConnected(userId);
    }

    @Test
    @DisplayName("사용자 세션 정보 조회")
    void getSessionInfo() {
        // given
        // [FIX] 변경된 DTO의 정적 팩토리 메서드를 사용하여 객체 생성
        WebSocketSessionInfo sessionInfo = WebSocketSessionInfo.createNewSession(userId, username, sessionId);
        given(userSessionService.getSessionInfo(userId)).willReturn(sessionInfo);

        // when
        WebSocketSessionInfo result = sessionManager.getSessionInfo(userId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.username()).isEqualTo(username);
        verify(userSessionService).getSessionInfo(userId);
    }

    @Test
    @DisplayName("Heartbeat 처리")
    void updateLastActivity() {
        // when
        sessionManager.updateLastActivity(userId);

        // then
        verify(userSessionService).processHeartbeat(userId);
    }

    @Test
    @DisplayName("전체 온라인 사용자 수 조회")
    void getTotalOnlineUserCount() {
        // given
        long expectedCount = 42L;
        given(userSessionService.getTotalOnlineUserCount()).willReturn(expectedCount);

        // when
        long result = sessionManager.getTotalOnlineUserCount();

        // then
        assertThat(result).isEqualTo(expectedCount);
        verify(userSessionService).getTotalOnlineUserCount();
    }
}
