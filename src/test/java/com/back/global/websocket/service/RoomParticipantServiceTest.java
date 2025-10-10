package com.back.global.websocket.service;

import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
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

import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoomParticipantService 단위 테스트")
class RoomParticipantServiceTest {

    @Mock
    private RedisSessionStore redisSessionStore;

    @InjectMocks
    private RoomParticipantService roomParticipantService;

    private Long userId;
    private String username;
    private String sessionId;
    private Long roomId;
    private WebSocketSessionInfo sessionInfo;

    @BeforeEach
    void setUp() {
        userId = 1L;
        username = "testUser";
        sessionId = "test-session-123";
        roomId = 100L;
        sessionInfo = WebSocketSessionInfo.createNewSession(userId, username, sessionId);
    }

    @Test
    @DisplayName("방 입장 - 정상 케이스 (첫 입장)")
    void t1() {
        // given
        given(redisSessionStore.getUserSession(userId)).willReturn(sessionInfo);

        // when
        roomParticipantService.enterRoom(userId, roomId);

        // then
        ArgumentCaptor<WebSocketSessionInfo> sessionCaptor = ArgumentCaptor.forClass(WebSocketSessionInfo.class);
        verify(redisSessionStore).saveUserSession(eq(userId), sessionCaptor.capture());
        verify(redisSessionStore).addUserToRoom(roomId, userId);

        WebSocketSessionInfo updatedSession = sessionCaptor.getValue();
        assertThat(updatedSession.currentRoomId()).isEqualTo(roomId);
        assertThat(updatedSession.userId()).isEqualTo(userId);
        assertThat(updatedSession.username()).isEqualTo(username);
    }

    @Test
    @DisplayName("방 입장 - 기존 방에서 자동 퇴장 후 새 방 입장")
    void t2() {
        // given
        Long oldRoomId = 200L;
        WebSocketSessionInfo sessionWithOldRoom = sessionInfo.withRoomId(oldRoomId);
        given(redisSessionStore.getUserSession(userId))
                .willReturn(sessionWithOldRoom)  // 첫 번째 호출 (입장 시)
                .willReturn(sessionWithOldRoom); // 두 번째 호출 (퇴장 시)

        // when
        roomParticipantService.enterRoom(userId, roomId);

        // then
        // 기존 방 퇴장 확인
        verify(redisSessionStore).removeUserFromRoom(oldRoomId, userId);

        // 새 방 입장 확인
        verify(redisSessionStore).addUserToRoom(roomId, userId);

        // 세션 업데이트 2번 (퇴장 시 1번, 입장 시 1번)
        ArgumentCaptor<WebSocketSessionInfo> sessionCaptor = ArgumentCaptor.forClass(WebSocketSessionInfo.class);
        verify(redisSessionStore, times(2)).saveUserSession(eq(userId), sessionCaptor.capture());

        WebSocketSessionInfo finalSession = sessionCaptor.getAllValues().get(1);
        assertThat(finalSession.currentRoomId()).isEqualTo(roomId);
    }

    @Test
    @DisplayName("방 입장 - 세션 정보 없음 (예외 발생)")
    void t3() {
        // given
        given(redisSessionStore.getUserSession(userId)).willReturn(null);

        // when & then
        assertThatThrownBy(() -> roomParticipantService.enterRoom(userId, roomId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WS_SESSION_NOT_FOUND);

        verify(redisSessionStore, never()).addUserToRoom(anyLong(), anyLong());
        verify(redisSessionStore, never()).saveUserSession(anyLong(), any());
    }

    @Test
    @DisplayName("방 퇴장 - 정상 케이스")
    void t4() {
        // given
        WebSocketSessionInfo sessionWithRoom = sessionInfo.withRoomId(roomId);
        given(redisSessionStore.getUserSession(userId)).willReturn(sessionWithRoom);

        // when
        roomParticipantService.exitRoom(userId, roomId);

        // then
        ArgumentCaptor<WebSocketSessionInfo> sessionCaptor = ArgumentCaptor.forClass(WebSocketSessionInfo.class);
        verify(redisSessionStore).saveUserSession(eq(userId), sessionCaptor.capture());
        verify(redisSessionStore).removeUserFromRoom(roomId, userId);

        WebSocketSessionInfo updatedSession = sessionCaptor.getValue();
        assertThat(updatedSession.currentRoomId()).isNull();
    }

    @Test
    @DisplayName("방 퇴장 - 세션 정보 없음 (퇴장 처리는 계속 진행)")
    void t5() {
        // given
        given(redisSessionStore.getUserSession(userId)).willReturn(null);

        // when
        roomParticipantService.exitRoom(userId, roomId);

        // then
        verify(redisSessionStore).removeUserFromRoom(roomId, userId);
        verify(redisSessionStore, never()).saveUserSession(anyLong(), any());
    }

    @Test
    @DisplayName("현재 방 ID 조회 - 방 있음")
    void t6() {
        // given
        WebSocketSessionInfo sessionWithRoom = sessionInfo.withRoomId(roomId);
        given(redisSessionStore.getUserSession(userId)).willReturn(sessionWithRoom);

        // when
        Long result = roomParticipantService.getCurrentRoomId(userId);

        // then
        assertThat(result).isEqualTo(roomId);
        verify(redisSessionStore).getUserSession(userId);
    }

    @Test
    @DisplayName("현재 방 ID 조회 - 방 없음")
    void t7() {
        // given
        given(redisSessionStore.getUserSession(userId)).willReturn(sessionInfo);

        // when
        Long result = roomParticipantService.getCurrentRoomId(userId);

        // then
        assertThat(result).isNull();
        verify(redisSessionStore).getUserSession(userId);
    }

    @Test
    @DisplayName("현재 방 ID 조회 - 세션 없음")
    void t8() {
        // given
        given(redisSessionStore.getUserSession(userId)).willReturn(null);

        // when
        Long result = roomParticipantService.getCurrentRoomId(userId);

        // then
        assertThat(result).isNull();
        verify(redisSessionStore).getUserSession(userId);
    }

    @Test
    @DisplayName("방의 참가자 목록 조회")
    void t9() {
        // given
        Set<Long> expectedParticipants = Set.of(1L, 2L, 3L);
        given(redisSessionStore.getRoomUsers(roomId)).willReturn(expectedParticipants);

        // when
        Set<Long> result = roomParticipantService.getParticipants(roomId);

        // then
        assertThat(result).containsExactlyInAnyOrderElementsOf(expectedParticipants);
        verify(redisSessionStore).getRoomUsers(roomId);
    }

    @Test
    @DisplayName("방의 참가자 목록 조회 - 빈 방")
    void t10() {
        // given
        given(redisSessionStore.getRoomUsers(roomId)).willReturn(Set.of());

        // when
        Set<Long> result = roomParticipantService.getParticipants(roomId);

        // then
        assertThat(result).isEmpty();
        verify(redisSessionStore).getRoomUsers(roomId);
    }

    @Test
    @DisplayName("방의 참가자 수 조회")
    void t11() {
        // given
        long expectedCount = 5L;
        given(redisSessionStore.getRoomUserCount(roomId)).willReturn(expectedCount);

        // when
        long result = roomParticipantService.getParticipantCount(roomId);

        // then
        assertThat(result).isEqualTo(expectedCount);
        verify(redisSessionStore).getRoomUserCount(roomId);
    }

    @Test
    @DisplayName("방의 참가자 수 조회 - 빈 방")
    void t12() {
        // given
        given(redisSessionStore.getRoomUserCount(roomId)).willReturn(0L);

        // when
        long result = roomParticipantService.getParticipantCount(roomId);

        // then
        assertThat(result).isZero();
        verify(redisSessionStore).getRoomUserCount(roomId);
    }

    @Test
    @DisplayName("사용자가 특정 방에 참여 중인지 확인 - 참여 중")
    void t13() {
        // given
        WebSocketSessionInfo sessionWithRoom = sessionInfo.withRoomId(roomId);
        given(redisSessionStore.getUserSession(userId)).willReturn(sessionWithRoom);

        // when
        boolean result = roomParticipantService.isUserInRoom(userId, roomId);

        // then
        assertThat(result).isTrue();
        verify(redisSessionStore).getUserSession(userId);
    }

    @Test
    @DisplayName("사용자가 특정 방에 참여 중인지 확인 - 다른 방에 참여 중")
    void t14() {
        // given
        Long differentRoomId = 999L;
        WebSocketSessionInfo sessionWithDifferentRoom = sessionInfo.withRoomId(differentRoomId);
        given(redisSessionStore.getUserSession(userId)).willReturn(sessionWithDifferentRoom);

        // when
        boolean result = roomParticipantService.isUserInRoom(userId, roomId);

        // then
        assertThat(result).isFalse();
        verify(redisSessionStore).getUserSession(userId);
    }

    @Test
    @DisplayName("사용자가 특정 방에 참여 중인지 확인 - 어떤 방에도 없음")
    void t15() {
        // given
        given(redisSessionStore.getUserSession(userId)).willReturn(sessionInfo);

        // when
        boolean result = roomParticipantService.isUserInRoom(userId, roomId);

        // then
        assertThat(result).isFalse();
        verify(redisSessionStore).getUserSession(userId);
    }

    @Test
    @DisplayName("사용자가 특정 방에 참여 중인지 확인 - 세션 없음")
    void t16() {
        // given
        given(redisSessionStore.getUserSession(userId)).willReturn(null);

        // when
        boolean result = roomParticipantService.isUserInRoom(userId, roomId);

        // then
        assertThat(result).isFalse();
        verify(redisSessionStore).getUserSession(userId);
    }

    @Test
    @DisplayName("모든 방에서 퇴장 - 현재 방 있음")
    void t17() {
        // given
        WebSocketSessionInfo sessionWithRoom = sessionInfo.withRoomId(roomId);
        given(redisSessionStore.getUserSession(userId)).willReturn(sessionWithRoom);

        // when
        roomParticipantService.exitAllRooms(userId);

        // then
        verify(redisSessionStore, times(2)).getUserSession(userId);
        verify(redisSessionStore).removeUserFromRoom(roomId, userId);
        verify(redisSessionStore).saveUserSession(eq(userId), any(WebSocketSessionInfo.class));
    }

    @Test
    @DisplayName("모든 방에서 퇴장 - 현재 방 없음")
    void t18() {
        // given
        given(redisSessionStore.getUserSession(userId)).willReturn(sessionInfo);

        // when
        roomParticipantService.exitAllRooms(userId);

        // then
        verify(redisSessionStore).getUserSession(userId);
        verify(redisSessionStore, never()).removeUserFromRoom(anyLong(), anyLong());
        verify(redisSessionStore, never()).saveUserSession(anyLong(), any());
    }

    @Test
    @DisplayName("모든 방에서 퇴장 - 세션 없음")
    void t19() {
        // given
        given(redisSessionStore.getUserSession(userId)).willReturn(null);

        // when
        roomParticipantService.exitAllRooms(userId);

        // then
        verify(redisSessionStore).getUserSession(userId);
        verify(redisSessionStore, never()).removeUserFromRoom(anyLong(), anyLong());
    }

    @Test
    @DisplayName("모든 방에서 퇴장 - 예외 발생해도 에러를 던지지 않음")
    void t20() {
        // given
        given(redisSessionStore.getUserSession(userId))
                .willThrow(new RuntimeException("Redis connection failed"));

        // when & then - 예외가 발생해도 메서드는 정상 종료되어야 함
        assertThatCode(() -> roomParticipantService.exitAllRooms(userId))
                .doesNotThrowAnyException();

        verify(redisSessionStore).getUserSession(userId);
    }

    @Test
    @DisplayName("같은 방에 재입장 시도")
    void t21() {
        // given
        WebSocketSessionInfo sessionWithRoom = sessionInfo.withRoomId(roomId);
        given(redisSessionStore.getUserSession(userId)).willReturn(sessionWithRoom);

        // when
        roomParticipantService.enterRoom(userId, roomId);

        // then
        // 같은 방이므로 퇴장 처리가 발생함
        verify(redisSessionStore).removeUserFromRoom(roomId, userId);
        verify(redisSessionStore).addUserToRoom(roomId, userId);

        // 세션 업데이트는 2번 (퇴장 + 입장)
        verify(redisSessionStore, times(2)).saveUserSession(eq(userId), any(WebSocketSessionInfo.class));
    }

    @Test
    @DisplayName("방 A → 방 B → 방 C 연속 이동")
    void t22() {
        // given
        Long roomA = 100L;
        Long roomB = 200L;
        Long roomC = 300L;

        WebSocketSessionInfo session1 = sessionInfo;
        WebSocketSessionInfo sessionInA = session1.withRoomId(roomA);
        WebSocketSessionInfo sessionInB = sessionInA.withRoomId(roomB);

        given(redisSessionStore.getUserSession(userId))
                .willReturn(session1)      // 첫 번째 입장 (방 A)
                .willReturn(sessionInA)    // 두 번째 입장 (방 B) - 기존 방 A에서 퇴장
                .willReturn(sessionInA)    // 방 A 퇴장 처리
                .willReturn(sessionInB)    // 세 번째 입장 (방 C) - 기존 방 B에서 퇴장
                .willReturn(sessionInB);   // 방 B 퇴장 처리

        // when
        roomParticipantService.enterRoom(userId, roomA);
        roomParticipantService.enterRoom(userId, roomB);
        roomParticipantService.enterRoom(userId, roomC);

        // then
        verify(redisSessionStore).addUserToRoom(roomA, userId);
        verify(redisSessionStore).addUserToRoom(roomB, userId);
        verify(redisSessionStore).addUserToRoom(roomC, userId);

        verify(redisSessionStore).removeUserFromRoom(roomA, userId);
        verify(redisSessionStore).removeUserFromRoom(roomB, userId);
    }

    @Test
    @DisplayName("방 입장 후 명시적 퇴장")
    void t23() {
        // given
        WebSocketSessionInfo sessionWithoutRoom = sessionInfo;
        WebSocketSessionInfo sessionWithRoom = sessionInfo.withRoomId(roomId);

        given(redisSessionStore.getUserSession(userId))
                .willReturn(sessionWithoutRoom)  // 입장 시
                .willReturn(sessionWithRoom);     // 퇴장 시

        // when
        roomParticipantService.enterRoom(userId, roomId);
        roomParticipantService.exitRoom(userId, roomId);

        // then
        verify(redisSessionStore).addUserToRoom(roomId, userId);
        verify(redisSessionStore).removeUserFromRoom(roomId, userId);

        ArgumentCaptor<WebSocketSessionInfo> captor = ArgumentCaptor.forClass(WebSocketSessionInfo.class);
        verify(redisSessionStore, times(2)).saveUserSession(eq(userId), captor.capture());

        // 입장 시 방 ID가 설정됨
        assertThat(captor.getAllValues().get(0).currentRoomId()).isEqualTo(roomId);
        // 퇴장 시 방 ID가 null이 됨
        assertThat(captor.getAllValues().get(1).currentRoomId()).isNull();
    }
}