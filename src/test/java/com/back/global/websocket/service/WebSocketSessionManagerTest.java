package com.back.global.websocket.service;

import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import com.back.global.websocket.dto.WebSocketSessionInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocketSessionManager 단위 테스트")
class WebSocketSessionManagerTest {

    @Mock
    private UserSessionService userSessionService;

    @Mock
    private RoomParticipantService roomParticipantService;

    @InjectMocks
    private WebSocketSessionManager sessionManager;

    private Long userId;
    private String username;
    private String sessionId;
    private Long roomId;

    @BeforeEach
    void setUp() {
        userId = 1L;
        username = "testuser";
        sessionId = "test-session-123";
        roomId = 100L;
    }

    @Test
    @DisplayName("세션 추가")
    void addSession() {
        // when
        sessionManager.addSession(userId, username, sessionId);

        // then
        verify(userSessionService).registerSession(userId, username, sessionId);
    }

    @Test
    @DisplayName("세션 제거 - 정상 케이스")
    void removeSession_Success() {
        // given
        given(userSessionService.getUserIdBySessionId(sessionId)).willReturn(userId);

        // when
        sessionManager.removeSession(sessionId);

        // then
        verify(userSessionService).getUserIdBySessionId(sessionId);
        verify(roomParticipantService).exitAllRooms(userId);
        verify(userSessionService).terminateSession(sessionId);
    }

    @Test
    @DisplayName("세션 제거 - 존재하지 않는 세션")
    void removeSession_NotFound() {
        // given
        given(userSessionService.getUserIdBySessionId(sessionId)).willReturn(null);

        // when
        sessionManager.removeSession(sessionId);

        // then
        verify(userSessionService).getUserIdBySessionId(sessionId);
        verify(roomParticipantService, never()).exitAllRooms(anyLong());
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
        // 생성자 변경
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

    @Test
    @DisplayName("방 입장")
    void joinRoom() {
        // when
        sessionManager.joinRoom(userId, roomId);

        // then
        verify(roomParticipantService).enterRoom(userId, roomId);
    }

    @Test
    @DisplayName("방 퇴장")
    void leaveRoom() {
        // when
        sessionManager.leaveRoom(userId, roomId);

        // then
        verify(roomParticipantService).exitRoom(userId, roomId);
    }

    @Test
    @DisplayName("방의 온라인 사용자 수 조회")
    void getRoomOnlineUserCount() {
        // given
        long expectedCount = 10L;
        given(roomParticipantService.getParticipantCount(roomId)).willReturn(expectedCount);

        // when
        long result = sessionManager.getRoomOnlineUserCount(roomId);

        // then
        assertThat(result).isEqualTo(expectedCount);
        verify(roomParticipantService).getParticipantCount(roomId);
    }

    @Test
    @DisplayName("방의 온라인 사용자 목록 조회")
    void getOnlineUsersInRoom() {
        // given
        Set<Long> expectedUsers = Set.of(1L, 2L, 3L);
        given(roomParticipantService.getParticipants(roomId)).willReturn(expectedUsers);

        // when
        Set<Long> result = sessionManager.getOnlineUsersInRoom(roomId);

        // then
        assertThat(result).containsExactlyInAnyOrderElementsOf(expectedUsers);
        verify(roomParticipantService).getParticipants(roomId);
    }

    @Test
    @DisplayName("사용자의 현재 방 조회")
    void getUserCurrentRoomId() {
        // given
        given(roomParticipantService.getCurrentRoomId(userId)).willReturn(roomId);

        // when
        Long result = sessionManager.getUserCurrentRoomId(userId);

        // then
        assertThat(result).isEqualTo(roomId);
        verify(roomParticipantService).getCurrentRoomId(userId);
    }

    @Test
    @DisplayName("사용자가 특정 방에 참여 중인지 확인")
    void isUserInRoom() {
        // given
        given(roomParticipantService.isUserInRoom(userId, roomId)).willReturn(true);

        // when
        boolean result = sessionManager.isUserInRoom(userId, roomId);

        // then
        assertThat(result).isTrue();
        verify(roomParticipantService).isUserInRoom(userId, roomId);
    }

    @Test
    @DisplayName("전체 플로우: 연결 → 방 입장 → Heartbeat → 방 퇴장 → 연결 종료")
    void fullLifecycleFlow() {
        // given
        given(userSessionService.getUserIdBySessionId(sessionId)).willReturn(userId);

        // when & then
        // 1. 연결
        sessionManager.addSession(userId, username, sessionId);
        verify(userSessionService).registerSession(userId, username, sessionId);

        // 2. 방 입장
        sessionManager.joinRoom(userId, roomId);
        verify(roomParticipantService).enterRoom(userId, roomId);

        // 3. Heartbeat
        sessionManager.updateLastActivity(userId);
        verify(userSessionService).processHeartbeat(userId);

        // 4. 방 퇴장
        sessionManager.leaveRoom(userId, roomId);
        verify(roomParticipantService).exitRoom(userId, roomId);

        // 5. 연결 종료
        sessionManager.removeSession(sessionId);
        verify(roomParticipantService).exitAllRooms(userId);
        verify(userSessionService).terminateSession(sessionId);
    }

    @Test
    @DisplayName("전체 플로우: 연결 → 방 A 입장 → 방 B 이동 → 연결 종료")
    void fullLifecycleFlow_RoomTransition() {
        // given
        Long roomA = 100L;
        Long roomB = 200L;
        given(userSessionService.getUserIdBySessionId(sessionId)).willReturn(userId);

        // when & then
        // 1. 연결
        sessionManager.addSession(userId, username, sessionId);
        verify(userSessionService).registerSession(userId, username, sessionId);

        // 2. 방 A 입장
        sessionManager.joinRoom(userId, roomA);
        verify(roomParticipantService).enterRoom(userId, roomA);

        // 3. 방 B로 이동 (자동으로 방 A 퇴장)
        sessionManager.joinRoom(userId, roomB);
        verify(roomParticipantService).enterRoom(userId, roomB);

        // 4. 연결 종료 (모든 방에서 퇴장)
        sessionManager.removeSession(sessionId);
        verify(roomParticipantService).exitAllRooms(userId);
        verify(userSessionService).terminateSession(sessionId);
    }

    @Test
    @DisplayName("여러 사용자의 동시 세션 관리")
    void multipleUsersSessions() {
        // given
        Long userId1 = 1L;
        Long userId2 = 2L;
        Long userId3 = 3L;

        String username1 = "user1";
        String username2 = "user2";
        String username3 = "user3";

        String sessionId1 = "session-1";
        String sessionId2 = "session-2";
        String sessionId3 = "session-3";

        // when
        sessionManager.addSession(userId1, username1, sessionId1);
        sessionManager.addSession(userId2, username2, sessionId2);
        sessionManager.addSession(userId3, username3, sessionId3);

        // then
        verify(userSessionService).registerSession(userId1, username1, sessionId1);
        verify(userSessionService).registerSession(userId2, username2, sessionId2);
        verify(userSessionService).registerSession(userId3, username3, sessionId3);
    }

    @Test
    @DisplayName("여러 사용자가 같은 방에 입장")
    void multipleUsersInSameRoom() {
        // given
        Long userId1 = 1L;
        Long userId2 = 2L;
        Long userId3 = 3L;

        // when
        sessionManager.joinRoom(userId1, roomId);
        sessionManager.joinRoom(userId2, roomId);
        sessionManager.joinRoom(userId3, roomId);

        // then
        verify(roomParticipantService).enterRoom(userId1, roomId);
        verify(roomParticipantService).enterRoom(userId2, roomId);
        verify(roomParticipantService).enterRoom(userId3, roomId);
    }

    @Test
    @DisplayName("중복 연결 시도 (기존 세션 종료 후 새 세션 등록)")
    void duplicateConnection() {
        // given
        String newSessionId = "new-session-456";

        // when
        sessionManager.addSession(userId, username, sessionId);
        sessionManager.addSession(userId, username, newSessionId);

        // then
        verify(userSessionService).registerSession(userId, username, sessionId);
        verify(userSessionService).registerSession(userId, username, newSessionId);
        // UserSessionService 내부에서 기존 세션 종료 처리
    }

    @Test
    @DisplayName("비정상 종료 시나리오: 명시적 퇴장 없이 연결 종료")
    void abnormalDisconnection() {
        // given
        given(userSessionService.getUserIdBySessionId(sessionId)).willReturn(userId);

        // when
        sessionManager.addSession(userId, username, sessionId);
        sessionManager.joinRoom(userId, roomId);
        // 명시적 leaveRoom 없이 바로 연결 종료
        sessionManager.removeSession(sessionId);

        // then
        verify(roomParticipantService).enterRoom(userId, roomId);
        verify(roomParticipantService).exitAllRooms(userId); // 모든 방에서 자동 퇴장
        verify(userSessionService).terminateSession(sessionId);
    }

    @Test
    @DisplayName("방 입장 전 상태 조회")
    void queryBeforeJoinRoom() {
        // given
        given(roomParticipantService.getCurrentRoomId(userId)).willReturn(null);
        given(roomParticipantService.isUserInRoom(userId, roomId)).willReturn(false);

        // when
        Long currentRoomId = sessionManager.getUserCurrentRoomId(userId);
        boolean isInRoom = sessionManager.isUserInRoom(userId, roomId);

        // then
        assertThat(currentRoomId).isNull();
        assertThat(isInRoom).isFalse();
        verify(roomParticipantService).getCurrentRoomId(userId);
        verify(roomParticipantService).isUserInRoom(userId, roomId);
    }

    @Test
    @DisplayName("방 입장 후 상태 조회")
    void queryAfterJoinRoom() {
        // given
        given(roomParticipantService.getCurrentRoomId(userId)).willReturn(roomId);
        given(roomParticipantService.isUserInRoom(userId, roomId)).willReturn(true);

        // when
        sessionManager.joinRoom(userId, roomId);
        Long currentRoomId = sessionManager.getUserCurrentRoomId(userId);
        boolean isInRoom = sessionManager.isUserInRoom(userId, roomId);

        // then
        assertThat(currentRoomId).isEqualTo(roomId);
        assertThat(isInRoom).isTrue();
        verify(roomParticipantService).enterRoom(userId, roomId);
        verify(roomParticipantService).getCurrentRoomId(userId);
        verify(roomParticipantService).isUserInRoom(userId, roomId);
    }

    @Test
    @DisplayName("Heartbeat 여러 번 호출")
    void multipleHeartbeats() {
        // when
        sessionManager.updateLastActivity(userId);
        sessionManager.updateLastActivity(userId);
        sessionManager.updateLastActivity(userId);

        // then
        verify(userSessionService, times(3)).processHeartbeat(userId);
    }

    @Test
    @DisplayName("빈 방의 사용자 목록 조회")
    void getOnlineUsersInRoom_EmptyRoom() {
        // given
        given(roomParticipantService.getParticipants(roomId)).willReturn(Set.of());

        // when
        Set<Long> result = sessionManager.getOnlineUsersInRoom(roomId);

        // then
        assertThat(result).isEmpty();
        verify(roomParticipantService).getParticipants(roomId);
    }

    @Test
    @DisplayName("온라인 사용자가 없을 때 전체 수 조회")
    void getTotalOnlineUserCount_NoUsers() {
        // given
        given(userSessionService.getTotalOnlineUserCount()).willReturn(0L);

        // when
        long result = sessionManager.getTotalOnlineUserCount();

        // then
        assertThat(result).isZero();
        verify(userSessionService).getTotalOnlineUserCount();
    }

    @Test
    @DisplayName("세션 제거 시 모든 정리 작업이 순서대로 실행됨")
    void removeSession_VerifyExecutionOrder() {
        // given
        given(userSessionService.getUserIdBySessionId(sessionId)).willReturn(userId);

        // when
        sessionManager.removeSession(sessionId);

        // then
        // InOrder를 사용하여 실행 순서 검증
        var inOrder = inOrder(userSessionService, roomParticipantService);
        inOrder.verify(userSessionService).getUserIdBySessionId(sessionId);
        inOrder.verify(roomParticipantService).exitAllRooms(userId);
        inOrder.verify(userSessionService).terminateSession(sessionId);
    }

    @Test
    @DisplayName("방 입장 실패 시 예외 전파")
    void joinRoom_ExceptionPropagation() {
        // given
        willThrow(new CustomException(ErrorCode.WS_SESSION_NOT_FOUND))
                .given(roomParticipantService).enterRoom(userId, roomId);

        // when & then
        assertThatThrownBy(() -> sessionManager.joinRoom(userId, roomId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WS_SESSION_NOT_FOUND);

        verify(roomParticipantService).enterRoom(userId, roomId);
    }

    @Test
    @DisplayName("통합 시나리오: 사용자 A와 B가 같은 방에서 만남")
    void integrationScenario_TwoUsersInSameRoom() {
        // given
        Long userA = 1L;
        Long userB = 2L;

        String usernameA = "userA";
        String usernameB = "userB";

        String sessionA = "session-A";
        String sessionB = "session-B";

        given(roomParticipantService.getParticipants(roomId))
                .willReturn(Set.of(userA))
                .willReturn(Set.of(userA, userB));

        // when & then
        // 1. 사용자 A 연결 및 방 입장
        sessionManager.addSession(userA, usernameA, sessionA);
        sessionManager.joinRoom(userA, roomId);

        Set<Long> usersAfterA = sessionManager.getOnlineUsersInRoom(roomId);
        assertThat(usersAfterA).containsExactly(userA);

        // 2. 사용자 B 연결 및 같은 방 입장
        sessionManager.addSession(userB, usernameB, sessionB);
        sessionManager.joinRoom(userB, roomId);

        Set<Long> usersAfterB = sessionManager.getOnlineUsersInRoom(roomId);
        assertThat(usersAfterB).containsExactlyInAnyOrder(userA, userB);

        // 3. 검증
        verify(userSessionService).registerSession(userA, usernameA, sessionA);
        verify(userSessionService).registerSession(userB, usernameB, sessionB);
        verify(roomParticipantService).enterRoom(userA, roomId);
        verify(roomParticipantService).enterRoom(userB, roomId);
        verify(roomParticipantService, times(2)).getParticipants(roomId);
    }

    @Test
    @DisplayName("통합 시나리오: 네트워크 불안정으로 재연결")
    void integrationScenario_Reconnection() {
        // given
        String newSessionId = "new-session-789";
        given(userSessionService.getUserIdBySessionId(sessionId)).willReturn(userId);

        // when & then
        // 1. 초기 연결 및 방 입장
        sessionManager.addSession(userId, username, sessionId);
        sessionManager.joinRoom(userId, roomId);

        // 2. 갑작스런 연결 끊김
        sessionManager.removeSession(sessionId);
        verify(roomParticipantService).exitAllRooms(userId);

        // 3. 재연결 (새 세션 ID로)
        sessionManager.addSession(userId, username, newSessionId);
        verify(userSessionService).registerSession(userId, username, newSessionId);

        // 4. 다시 방 입장
        sessionManager.joinRoom(userId, roomId);
        verify(roomParticipantService, times(2)).enterRoom(userId, roomId);
    }
}