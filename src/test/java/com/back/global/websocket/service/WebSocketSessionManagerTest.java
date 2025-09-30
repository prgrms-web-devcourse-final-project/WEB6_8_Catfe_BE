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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocketSessionManager 테스트")
class WebSocketSessionManagerTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private SetOperations<String, Object> setOperations;

    @InjectMocks
    private WebSocketSessionManager sessionManager;

    private final Long TEST_USER_ID = 123L;
    private final String TEST_SESSION_ID = "session-123";
    private final Long TEST_ROOM_ID = 456L;

    @BeforeEach
    void setUp() {
        // Mock 설정은 각 테스트에서 필요할 때만 수행
    }

    @Test
    @DisplayName("사용자 세션 등록 - 성공")
    void t1() {
        // given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null); // 기존 세션 없음

        // when
        assertThatNoException().isThrownBy(() ->
                sessionManager.addSession(TEST_USER_ID, TEST_SESSION_ID)
        );

        // then
        verify(valueOperations, times(2)).set(anyString(), any(), eq(Duration.ofMinutes(6)));
    }

    @Test
    @DisplayName("사용자 세션 등록 - 기존 세션 있을 때 제거 후 등록")
    void t2() {
        // given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        WebSocketSessionInfo existingSession = WebSocketSessionInfo.createNewSession(TEST_USER_ID, "old-session-123")
                .withUpdatedActivity(); // 활동 시간 업데이트

        // when
        when(valueOperations.get("ws:user:123")).thenReturn(existingSession);
        when(valueOperations.get("ws:session:old-session-123")).thenReturn(TEST_USER_ID);

        assertThatNoException().isThrownBy(() ->
                sessionManager.addSession(TEST_USER_ID, TEST_SESSION_ID)
        );

        // then
        verify(redisTemplate, atLeastOnce()).delete(anyString()); // 기존 세션 삭제
        verify(valueOperations, times(2)).set(anyString(), any(), eq(Duration.ofMinutes(6))); // 새 세션 등록
    }

    @Test
    @DisplayName("사용자 세션 등록 - Redis 오류 시 예외 발생")
    void t3() {
        // given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis connection failed"));

        // when & then
        assertThatThrownBy(() -> sessionManager.addSession(TEST_USER_ID, TEST_SESSION_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WS_CONNECTION_FAILED);
    }

    @Test
    @DisplayName("사용자 연결 상태 확인 - 연결됨")
    void t4() {
        // given
        when(redisTemplate.hasKey("ws:user:123")).thenReturn(true);

        // when
        boolean result = sessionManager.isUserConnected(TEST_USER_ID);

        // then
        assertThat(result).isTrue();
        verify(redisTemplate).hasKey("ws:user:123");
    }

    @Test
    @DisplayName("사용자 연결 상태 확인 - 연결되지 않음")
    void t5() {
        // given
        when(redisTemplate.hasKey("ws:user:123")).thenReturn(false);

        // when
        boolean result = sessionManager.isUserConnected(TEST_USER_ID);

        // then
        assertThat(result).isFalse();
        verify(redisTemplate).hasKey("ws:user:123");
    }

    @Test
    @DisplayName("사용자 연결 상태 확인 - Redis 오류 시 예외 발생")
    void t6() {
        // given
        when(redisTemplate.hasKey(anyString())).thenThrow(new RuntimeException("Redis error"));

        // when & then
        assertThatThrownBy(() -> sessionManager.isUserConnected(TEST_USER_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WS_REDIS_ERROR);
    }

    @Test
    @DisplayName("세션 정보 조회 - 성공")
    void t7() {
        // given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // 체이닝으로 세션 정보 생성
        WebSocketSessionInfo expectedSessionInfo = WebSocketSessionInfo
                .createNewSession(TEST_USER_ID, TEST_SESSION_ID)
                .withRoomId(TEST_ROOM_ID);

        when(valueOperations.get("ws:user:123")).thenReturn(expectedSessionInfo);

        // when
        WebSocketSessionInfo result = sessionManager.getSessionInfo(TEST_USER_ID);

        // then
        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(TEST_USER_ID);
        assertThat(result.sessionId()).isEqualTo(TEST_SESSION_ID);
        assertThat(result.currentRoomId()).isEqualTo(TEST_ROOM_ID);
    }

    @Test
    @DisplayName("세션 정보 조회 - 세션이 없음")
    void t8() {
        // given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("ws:user:123")).thenReturn(null);

        // when
        WebSocketSessionInfo result = sessionManager.getSessionInfo(TEST_USER_ID);

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("활동 시간 업데이트 - 성공")
    void t9() {
        // given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        WebSocketSessionInfo sessionInfo = WebSocketSessionInfo
                .createNewSession(TEST_USER_ID, TEST_SESSION_ID);

        when(valueOperations.get("ws:user:123")).thenReturn(sessionInfo);

        // when
        assertThatNoException().isThrownBy(() ->
                sessionManager.updateLastActivity(TEST_USER_ID)
        );

        // then
        verify(valueOperations).set(eq("ws:user:123"), any(WebSocketSessionInfo.class), eq(Duration.ofMinutes(6)));
    }

    @Test
    @DisplayName("활동 시간 업데이트 - 세션이 없을 때")
    void t10() {
        // given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("ws:user:123")).thenReturn(null);

        // when & then
        assertThatNoException().isThrownBy(() ->
                sessionManager.updateLastActivity(TEST_USER_ID)
        );

        // 세션이 없으면 업데이트하지 않음
        verify(valueOperations, never()).set(anyString(), any(), any(Duration.class));
    }

    @Test
    @DisplayName("방 입장 - 성공")
    void t11() {
        // given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        WebSocketSessionInfo sessionInfo = WebSocketSessionInfo
                .createNewSession(TEST_USER_ID, TEST_SESSION_ID);

        when(valueOperations.get("ws:user:123")).thenReturn(sessionInfo);

        // when
        assertThatNoException().isThrownBy(() ->
                sessionManager.joinRoom(TEST_USER_ID, TEST_ROOM_ID)
        );

        // then
        verify(setOperations).add("ws:room:456:users", TEST_USER_ID);
        verify(redisTemplate).expire("ws:room:456:users", Duration.ofMinutes(6));
        verify(valueOperations).set(eq("ws:user:123"), any(WebSocketSessionInfo.class), eq(Duration.ofMinutes(6)));
    }

    @Test
    @DisplayName("방 입장 - 기존 방에서 자동 퇴장 후 새 방 입장")
    void t12() {
        // given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        Long previousRoomId = 999L;

        WebSocketSessionInfo sessionInfo = WebSocketSessionInfo
                .createNewSession(TEST_USER_ID, TEST_SESSION_ID)
                .withRoomId(previousRoomId);

        when(valueOperations.get("ws:user:123")).thenReturn(sessionInfo);

        // when
        assertThatNoException().isThrownBy(() ->
                sessionManager.joinRoom(TEST_USER_ID, TEST_ROOM_ID)
        );

        // then
        // 이전 방에서 퇴장
        verify(setOperations).remove("ws:room:999:users", TEST_USER_ID);

        // 새 방에 입장
        verify(setOperations).add("ws:room:456:users", TEST_USER_ID);
    }

    @Test
    @DisplayName("방 퇴장 - 성공")
    void t13() {
        // given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        WebSocketSessionInfo sessionInfo = WebSocketSessionInfo
                .createNewSession(TEST_USER_ID, TEST_SESSION_ID)
                .withRoomId(TEST_ROOM_ID);

        when(valueOperations.get("ws:user:123")).thenReturn(sessionInfo);

        // when
        assertThatNoException().isThrownBy(() ->
                sessionManager.leaveRoom(TEST_USER_ID, TEST_ROOM_ID)
        );

        // then
        verify(setOperations).remove("ws:room:456:users", TEST_USER_ID);
        verify(valueOperations).set(eq("ws:user:123"), any(WebSocketSessionInfo.class), eq(Duration.ofMinutes(6)));
    }

    @Test
    @DisplayName("방 온라인 사용자 수 조회 - 성공")
    void t14() {
        // given
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.size("ws:room:456:users")).thenReturn(5L);

        // when
        long result = sessionManager.getRoomOnlineUserCount(TEST_ROOM_ID);

        // then
        assertThat(result).isEqualTo(5L);
        verify(setOperations).size("ws:room:456:users");
    }

    @Test
    @DisplayName("방 온라인 사용자 수 조회 - 사용자가 없을 때")
    void t15() {
        // given
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.size("ws:room:456:users")).thenReturn(null);

        // when
        long result = sessionManager.getRoomOnlineUserCount(TEST_ROOM_ID);

        // then
        assertThat(result).isEqualTo(0L);
    }

    @Test
    @DisplayName("방 온라인 사용자 목록 조회 - 성공")
    void t16() {
        // given
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        Set<Object> expectedUserIds = Set.of(123L, 456L, 789L);
        when(setOperations.members("ws:room:456:users")).thenReturn(expectedUserIds);

        // when
        Set<Long> result = sessionManager.getOnlineUsersInRoom(TEST_ROOM_ID);

        // then
        assertThat(result).containsExactlyInAnyOrder(123L, 456L, 789L);
        verify(setOperations).members("ws:room:456:users");
    }

    @Test
    @DisplayName("방 온라인 사용자 목록 조회 - 빈 방")
    void t17() {
        // given
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members("ws:room:456:users")).thenReturn(null);

        // when
        Set<Long> result = sessionManager.getOnlineUsersInRoom(TEST_ROOM_ID);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("전체 온라인 사용자 수 조회 - 성공")
    void t18() {
        // given
        Set<String> userKeys = Set.of("ws:user:123", "ws:user:456", "ws:user:789");
        when(redisTemplate.keys("ws:user:*")).thenReturn(userKeys);

        // when
        long result = sessionManager.getTotalOnlineUserCount();

        // then
        assertThat(result).isEqualTo(3L);
    }

    @Test
    @DisplayName("전체 온라인 사용자 수 조회 - Redis 오류 시 0 반환")
    void t19() {
        // given
        when(redisTemplate.keys("ws:user:*")).thenThrow(new RuntimeException("Redis error"));

        // when
        long result = sessionManager.getTotalOnlineUserCount();

        // then
        assertThat(result).isEqualTo(0L); // 예외 대신 0 반환
    }

    @Test
    @DisplayName("사용자 현재 방 ID 조회 - 성공")
    void t20() {
        // given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        WebSocketSessionInfo sessionInfo = WebSocketSessionInfo
                .createNewSession(TEST_USER_ID, TEST_SESSION_ID)
                .withRoomId(TEST_ROOM_ID);

        when(valueOperations.get("ws:user:123")).thenReturn(sessionInfo);

        // when
        Long result = sessionManager.getUserCurrentRoomId(TEST_USER_ID);

        // then
        assertThat(result).isEqualTo(TEST_ROOM_ID);
    }

    @Test
    @DisplayName("사용자 현재 방 ID 조회 - 방에 입장하지 않음")
    void t21() {
        // given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // 방 정보 없는 세션
        WebSocketSessionInfo sessionInfo = WebSocketSessionInfo
                .createNewSession(TEST_USER_ID, TEST_SESSION_ID); // currentRoomId는 null

        when(valueOperations.get("ws:user:123")).thenReturn(sessionInfo);

        // when
        Long result = sessionManager.getUserCurrentRoomId(TEST_USER_ID);

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("사용자 현재 방 ID 조회 - 세션이 없음")
    void t22() {
        // given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("ws:user:123")).thenReturn(null);

        // when
        Long result = sessionManager.getUserCurrentRoomId(TEST_USER_ID);

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("세션 제거 - 성공")
    void t23() {
        // given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(valueOperations.get("ws:session:session-123")).thenReturn(TEST_USER_ID);

        WebSocketSessionInfo sessionInfo = WebSocketSessionInfo
                .createNewSession(TEST_USER_ID, TEST_SESSION_ID)
                .withRoomId(TEST_ROOM_ID);
        when(valueOperations.get("ws:user:123")).thenReturn(sessionInfo);

        // when
        assertThatNoException().isThrownBy(() ->
                sessionManager.removeSession(TEST_SESSION_ID)
        );

        // then
        verify(setOperations).remove("ws:room:456:users", TEST_USER_ID); // 방에서 퇴장
        verify(redisTemplate, times(2)).delete(anyString()); // 세션 데이터 삭제
    }

    @Test
    @DisplayName("세션 제거 - 존재하지 않는 세션")
    void t24() {
        // given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("ws:session:session-123")).thenReturn(null);

        // when & then
        assertThatNoException().isThrownBy(() ->
                sessionManager.removeSession(TEST_SESSION_ID)
        );

        // 아무것도 삭제하지 않음
        verify(redisTemplate, never()).delete(anyString());
    }
}