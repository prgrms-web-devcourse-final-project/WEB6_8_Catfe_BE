package com.back.global.websocket.service;

import com.back.global.websocket.dto.WebSocketSessionInfo;
import com.back.global.websocket.store.RedisSessionStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserSessionService 단위 테스트")
class UserSessionServiceTest {

    @Mock
    private RedisSessionStore redisSessionStore;

    @InjectMocks
    private UserSessionService userSessionService;

    private Long userId;
    private String username;
    private String sessionId;
    private WebSocketSessionInfo sessionInfo;

    @BeforeEach
    void setUp() {
        userId = 1L;
        username = "testuser";
        sessionId = "test-session-123";
        sessionInfo = WebSocketSessionInfo.createNewSession(userId, username, sessionId);
    }

    @Test
    @DisplayName("새 세션 등록 - 기존 세션 없음")
    void t1() {
        // given
        given(redisSessionStore.getUserSession(userId)).willReturn(null);

        // when
        userSessionService.registerSession(userId, username,sessionId);

        // then
        ArgumentCaptor<WebSocketSessionInfo> sessionCaptor = ArgumentCaptor.forClass(WebSocketSessionInfo.class);
        verify(redisSessionStore).saveUserSession(eq(userId), sessionCaptor.capture());
        verify(redisSessionStore).saveSessionUserMapping(eq(sessionId), eq(userId));

        WebSocketSessionInfo savedSession = sessionCaptor.getValue();
        assertThat(savedSession.userId()).isEqualTo(userId);
        assertThat(savedSession.sessionId()).isEqualTo(sessionId);
        assertThat(savedSession.currentRoomId()).isNull();
        assertThat(savedSession.connectedAt()).isNotNull();
        assertThat(savedSession.lastActiveAt()).isNotNull();
    }

    @Test
    @DisplayName("새 세션 등록 - 기존 세션 있음 (기존 세션 종료 후 등록)")
    void t2() {
        // given
        String oldSessionId = "old-session-456";
        WebSocketSessionInfo oldSession = WebSocketSessionInfo.createNewSession(userId, username, oldSessionId);
        given(redisSessionStore.getUserSession(userId)).willReturn(oldSession);
        given(redisSessionStore.getUserIdBySession(oldSessionId)).willReturn(userId);

        // when
        userSessionService.registerSession(userId, username, sessionId); // username 전달

        // then
        verify(redisSessionStore).deleteUserSession(userId);
        verify(redisSessionStore).deleteSessionUserMapping(oldSessionId);
        verify(redisSessionStore).saveUserSession(eq(userId), any(WebSocketSessionInfo.class));
        verify(redisSessionStore).saveSessionUserMapping(eq(sessionId), eq(userId));
    }

    @Test
    @DisplayName("세션 종료 - 정상 케이스")
    void t3() {
        // given
        given(redisSessionStore.getUserIdBySession(sessionId)).willReturn(userId);

        // when
        userSessionService.terminateSession(sessionId);

        // then
        verify(redisSessionStore).deleteUserSession(userId);
        verify(redisSessionStore).deleteSessionUserMapping(sessionId);
    }

    @Test
    @DisplayName("세션 종료 - 존재하지 않는 세션")
    void t4() {
        // given
        given(redisSessionStore.getUserIdBySession(sessionId)).willReturn(null);

        // when
        userSessionService.terminateSession(sessionId);

        // then
        verify(redisSessionStore, never()).deleteUserSession(anyLong());
        verify(redisSessionStore, never()).deleteSessionUserMapping(anyString());
    }

    @Test
    @DisplayName("Heartbeat 처리 - 정상 케이스")
    void t5() {
        // given
        given(redisSessionStore.getUserSession(userId)).willReturn(sessionInfo);

        // when
        userSessionService.processHeartbeat(userId);

        // then
        ArgumentCaptor<WebSocketSessionInfo> sessionCaptor = ArgumentCaptor.forClass(WebSocketSessionInfo.class);
        verify(redisSessionStore).saveUserSession(eq(userId), sessionCaptor.capture());

        WebSocketSessionInfo updatedSession = sessionCaptor.getValue();
        assertThat(updatedSession.userId()).isEqualTo(userId);
        assertThat(updatedSession.sessionId()).isEqualTo(sessionId);
        // lastActiveAt이 업데이트되었는지 확인
        assertThat(updatedSession.lastActiveAt()).isAfterOrEqualTo(sessionInfo.lastActiveAt());
    }

    @Test
    @DisplayName("Heartbeat 처리 - 세션 정보 없음")
    void t6() {
        // given
        given(redisSessionStore.getUserSession(userId)).willReturn(null);

        // when
        userSessionService.processHeartbeat(userId);

        // then
        verify(redisSessionStore, never()).saveUserSession(anyLong(), any());
    }

    @Test
    @DisplayName("사용자 연결 상태 확인 - 연결됨")
    void t7() {
        // given
        given(redisSessionStore.existsUserSession(userId)).willReturn(true);

        // when
        boolean result = userSessionService.isConnected(userId);

        // then
        assertThat(result).isTrue();
        verify(redisSessionStore).existsUserSession(userId);
    }

    @Test
    @DisplayName("사용자 연결 상태 확인 - 연결 안 됨")
    void t8() {
        // given
        given(redisSessionStore.existsUserSession(userId)).willReturn(false);

        // when
        boolean result = userSessionService.isConnected(userId);

        // then
        assertThat(result).isFalse();
        verify(redisSessionStore).existsUserSession(userId);
    }

    @Test
    @DisplayName("세션 정보 조회 - 정상 케이스")
    void t9() {
        // given
        given(redisSessionStore.getUserSession(userId)).willReturn(sessionInfo);

        // when
        WebSocketSessionInfo result = userSessionService.getSessionInfo(userId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.sessionId()).isEqualTo(sessionId);
        verify(redisSessionStore).getUserSession(userId);
    }

    @Test
    @DisplayName("세션 정보 조회 - 세션 없음")
    void t10() {
        // given
        given(redisSessionStore.getUserSession(userId)).willReturn(null);

        // when
        WebSocketSessionInfo result = userSessionService.getSessionInfo(userId);

        // then
        assertThat(result).isNull();
        verify(redisSessionStore).getUserSession(userId);
    }

    @Test
    @DisplayName("세션ID로 사용자ID 조회 - 정상 케이스")
    void t11() {
        // given
        given(redisSessionStore.getUserIdBySession(sessionId)).willReturn(userId);

        // when
        Long result = userSessionService.getUserIdBySessionId(sessionId);

        // then
        assertThat(result).isEqualTo(userId);
        verify(redisSessionStore).getUserIdBySession(sessionId);
    }

    @Test
    @DisplayName("세션ID로 사용자ID 조회 - 세션 없음")
    void t12() {
        // given
        given(redisSessionStore.getUserIdBySession(sessionId)).willReturn(null);

        // when
        Long result = userSessionService.getUserIdBySessionId(sessionId);

        // then
        assertThat(result).isNull();
        verify(redisSessionStore).getUserIdBySession(sessionId);
    }

    @Test
    @DisplayName("사용자의 현재 방 ID 조회 - 방 있음")
    void t13() {
        // given
        Long roomId = 100L;
        WebSocketSessionInfo sessionWithRoom = sessionInfo.withRoomId(roomId);
        given(redisSessionStore.getUserSession(userId)).willReturn(sessionWithRoom);

        // when
        Long result = userSessionService.getCurrentRoomId(userId);

        // then
        assertThat(result).isEqualTo(roomId);
        verify(redisSessionStore).getUserSession(userId);
    }

    @Test
    @DisplayName("사용자의 현재 방 ID 조회 - 방 없음")
    void t14() {
        // given
        given(redisSessionStore.getUserSession(userId)).willReturn(sessionInfo);

        // when
        Long result = userSessionService.getCurrentRoomId(userId);

        // then
        assertThat(result).isNull();
        verify(redisSessionStore).getUserSession(userId);
    }

    @Test
    @DisplayName("사용자의 현재 방 ID 조회 - 세션 없음")
    void t15() {
        // given
        given(redisSessionStore.getUserSession(userId)).willReturn(null);

        // when
        Long result = userSessionService.getCurrentRoomId(userId);

        // then
        assertThat(result).isNull();
        verify(redisSessionStore).getUserSession(userId);
    }

    @Test
    @DisplayName("전체 온라인 사용자 수 조회")
    void t16() {
        // given
        long expectedCount = 42L;
        given(redisSessionStore.getTotalOnlineUserCount()).willReturn(expectedCount);

        // when
        long result = userSessionService.getTotalOnlineUserCount();

        // then
        assertThat(result).isEqualTo(expectedCount);
        verify(redisSessionStore).getTotalOnlineUserCount();
    }

    @Test
    @DisplayName("전체 온라인 사용자 수 조회 - 사용자 없음")
    void t17() {
        // given
        given(redisSessionStore.getTotalOnlineUserCount()).willReturn(0L);

        // when
        long result = userSessionService.getTotalOnlineUserCount();

        // then
        assertThat(result).isZero();
        verify(redisSessionStore).getTotalOnlineUserCount();
    }

    @Test
    @DisplayName("중복 세션 등록 시 기존 세션이 완전히 정리됨")
    void t18() {
        // given
        String oldSessionId = "old-session";
        Long oldRoomId = 999L;
        WebSocketSessionInfo oldSession = WebSocketSessionInfo.createNewSession(userId, username, oldSessionId)
                .withRoomId(oldRoomId);

        given(redisSessionStore.getUserSession(userId)).willReturn(oldSession);
        given(redisSessionStore.getUserIdBySession(oldSessionId)).willReturn(userId);

        // when
        userSessionService.registerSession(userId, username, sessionId); // username 전달

        // then
        verify(redisSessionStore).deleteUserSession(userId);
        verify(redisSessionStore).deleteSessionUserMapping(oldSessionId);
        verify(redisSessionStore).saveUserSession(eq(userId), any(WebSocketSessionInfo.class));
        verify(redisSessionStore).saveSessionUserMapping(eq(sessionId), eq(userId));
    }
}