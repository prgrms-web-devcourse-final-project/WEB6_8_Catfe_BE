package com.back.global.websocket.store;

import com.back.global.websocket.config.WebSocketConstants;
import com.back.global.websocket.dto.WebSocketSessionInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Set;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Testcontainers
@DisplayName("RedisSessionStore 통합 테스트")
class RedisSessionStoreTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private RedisSessionStore redisSessionStore;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp() {
        // Redis 초기화
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @AfterEach
    void tearDown() {
        // 테스트 후 정리
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Test
    @DisplayName("사용자 세션 저장 및 조회")
    void t1() {
        // given
        Long userId = 1L;
        String username = "testuser";
        String sessionId = "test-session-123";
        WebSocketSessionInfo sessionInfo = WebSocketSessionInfo.createNewSession(userId, username, sessionId);

        // when
        redisSessionStore.saveUserSession(userId, sessionInfo);
        WebSocketSessionInfo retrieved = redisSessionStore.getUserSession(userId);

        // then
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.userId()).isEqualTo(userId);
        assertThat(retrieved.sessionId()).isEqualTo(sessionId);
        assertThat(retrieved.currentRoomId()).isNull();
        assertThat(retrieved.connectedAt()).isNotNull();
        assertThat(retrieved.lastActiveAt()).isNotNull();
    }

    @Test
    @DisplayName("존재하지 않는 사용자 세션 조회 시 null 반환")
    void t2() {
        // given
        Long userId = 999L;

        // when
        WebSocketSessionInfo result = redisSessionStore.getUserSession(userId);

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("세션-사용자 매핑 저장 및 조회")
    void t3() {
        // given
        String sessionId = "test-session-456";
        Long userId = 2L;

        // when
        redisSessionStore.saveSessionUserMapping(sessionId, userId);
        Long retrievedUserId = redisSessionStore.getUserIdBySession(sessionId);

        // then
        assertThat(retrievedUserId).isEqualTo(userId);
    }

    @Test
    @DisplayName("존재하지 않는 세션으로 사용자 조회 시 null 반환")
    void t4() {
        // given
        String sessionId = "non-existent-session";

        // when
        Long result = redisSessionStore.getUserIdBySession(sessionId);

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("사용자 세션 삭제")
    void t5() {
        // given
        Long userId = 3L;
        String username = "deleteuser";
        String sessionId = "test-session-789";
        WebSocketSessionInfo sessionInfo = WebSocketSessionInfo.createNewSession(userId, username, sessionId);
        redisSessionStore.saveUserSession(userId, sessionInfo);

        // when
        redisSessionStore.deleteUserSession(userId);
        WebSocketSessionInfo result = redisSessionStore.getUserSession(userId);

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("세션-사용자 매핑 삭제")
    void t6() {
        // given
        String sessionId = "test-session-delete";
        Long userId = 4L;
        redisSessionStore.saveSessionUserMapping(sessionId, userId);

        // when
        redisSessionStore.deleteSessionUserMapping(sessionId);
        Long result = redisSessionStore.getUserIdBySession(sessionId);

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("사용자 세션 존재 여부 확인")
    void t7() {
        // given
        Long userId = 5L;
        String username = "existsuser";
        String sessionId = "test-session-exists";
        WebSocketSessionInfo sessionInfo = WebSocketSessionInfo.createNewSession(userId, username, sessionId);

        // when & then
        assertThat(redisSessionStore.existsUserSession(userId)).isFalse();

        redisSessionStore.saveUserSession(userId, sessionInfo);
        assertThat(redisSessionStore.existsUserSession(userId)).isTrue();

        redisSessionStore.deleteUserSession(userId);
        assertThat(redisSessionStore.existsUserSession(userId)).isFalse();
    }

    @Test
    @DisplayName("방에 사용자 추가")
    void t8() {
        // given
        Long roomId = 100L;
        Long userId = 6L;

        // when
        redisSessionStore.addUserToRoom(roomId, userId);
        Set<Long> roomUsers = redisSessionStore.getRoomUsers(roomId);

        // then
        assertThat(roomUsers).contains(userId);
        assertThat(roomUsers).hasSize(1);
    }

    @Test
    @DisplayName("방에 여러 사용자 추가")
    void t9() {
        // given
        Long roomId = 101L;
        Long userId1 = 7L;
        Long userId2 = 8L;
        Long userId3 = 9L;

        // when
        redisSessionStore.addUserToRoom(roomId, userId1);
        redisSessionStore.addUserToRoom(roomId, userId2);
        redisSessionStore.addUserToRoom(roomId, userId3);

        Set<Long> roomUsers = redisSessionStore.getRoomUsers(roomId);
        long userCount = redisSessionStore.getRoomUserCount(roomId);

        // then
        assertThat(roomUsers).containsExactlyInAnyOrder(userId1, userId2, userId3);
        assertThat(userCount).isEqualTo(3);
    }

    @Test
    @DisplayName("방에서 사용자 제거")
    void t10() {
        // given
        Long roomId = 102L;
        Long userId1 = 10L;
        Long userId2 = 11L;
        redisSessionStore.addUserToRoom(roomId, userId1);
        redisSessionStore.addUserToRoom(roomId, userId2);

        // when
        redisSessionStore.removeUserFromRoom(roomId, userId1);
        Set<Long> roomUsers = redisSessionStore.getRoomUsers(roomId);

        // then
        assertThat(roomUsers).containsExactly(userId2);
        assertThat(roomUsers).doesNotContain(userId1);
    }

    @Test
    @DisplayName("존재하지 않는 방의 사용자 목록 조회 시 빈 Set 반환")
    void t11() {
        // given
        Long roomId = 999L;

        // when
        Set<Long> roomUsers = redisSessionStore.getRoomUsers(roomId);

        // then
        assertThat(roomUsers).isEmpty();
    }

    @Test
    @DisplayName("방의 사용자 수 조회")
    void t12() {
        // given
        Long roomId = 103L;
        redisSessionStore.addUserToRoom(roomId, 12L);
        redisSessionStore.addUserToRoom(roomId, 13L);
        redisSessionStore.addUserToRoom(roomId, 14L);

        // when
        long count = redisSessionStore.getRoomUserCount(roomId);

        // then
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("존재하지 않는 방의 사용자 수는 0")
    void t13() {
        // given
        Long roomId = 999L;

        // when
        long count = redisSessionStore.getRoomUserCount(roomId);

        // then
        assertThat(count).isZero();
    }

    @Test
    @DisplayName("전체 온라인 사용자 수 조회")
    void t14() {
        // given & when
        // 세션 저장 대신, 카운터 증가 메서드를 직접 3번 호출
        redisSessionStore.incrementOnlineUserCount();
        redisSessionStore.incrementOnlineUserCount();
        redisSessionStore.incrementOnlineUserCount();

        // 카운터 값을 조회
        long totalCount = redisSessionStore.getTotalOnlineUserCount();

        // then
        // 증가된 카운터 값이 3인지 확인
        assertThat(totalCount).isEqualTo(3);
    }

    @Test
    @DisplayName("세션 정보에 방 ID 추가 후 저장 및 조회")
    void t15() {
        // given
        Long userId = 18L;
        String username = "roomuser";
        Long roomId = 200L;
        String sessionId = "session-with-room";
        WebSocketSessionInfo sessionInfo = WebSocketSessionInfo.createNewSession(userId, username, sessionId);
        WebSocketSessionInfo withRoom = sessionInfo.withRoomId(roomId);

        // when
        redisSessionStore.saveUserSession(userId, withRoom);
        WebSocketSessionInfo retrieved = redisSessionStore.getUserSession(userId);

        // then
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.currentRoomId()).isEqualTo(roomId);
        assertThat(retrieved.userId()).isEqualTo(userId);
        assertThat(retrieved.sessionId()).isEqualTo(sessionId);
    }

    @Test
    @DisplayName("세션 정보에서 방 ID 제거 후 저장 및 조회")
    void t16() {
        // given
        Long userId = 19L;
        String username = "removeroomuser";
        Long roomId = 201L;
        String sessionId = "session-remove-room";
        WebSocketSessionInfo sessionInfo = WebSocketSessionInfo.createNewSession(userId, username, sessionId);
        WebSocketSessionInfo withRoom = sessionInfo.withRoomId(roomId);
        redisSessionStore.saveUserSession(userId, withRoom);

        // when
        WebSocketSessionInfo withoutRoom = withRoom.withoutRoom();
        redisSessionStore.saveUserSession(userId, withoutRoom);
        WebSocketSessionInfo retrieved = redisSessionStore.getUserSession(userId);

        // then
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.currentRoomId()).isNull();
    }

    @Test
    @DisplayName("활동 시간 업데이트 후 저장 및 조회")
    void t17() throws InterruptedException {
        // given
        Long userId = 20L;
        String username = "activityuser";
        String sessionId = "session-activity-update";
        WebSocketSessionInfo sessionInfo = WebSocketSessionInfo.createNewSession(userId, username, sessionId);
        redisSessionStore.saveUserSession(userId, sessionInfo);

        // when
        Thread.sleep(100); // 시간 차이를 만들기 위해 대기
        WebSocketSessionInfo updatedSession = sessionInfo.withUpdatedActivity();
        redisSessionStore.saveUserSession(userId, updatedSession);
        WebSocketSessionInfo retrieved = redisSessionStore.getUserSession(userId);

        // then
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.lastActiveAt()).isAfter(sessionInfo.lastActiveAt());
    }

    @Test
    @DisplayName("중복 사용자를 같은 방에 추가해도 한 번만 저장됨")
    void t18() {
        // given
        Long roomId = 104L;
        Long userId = 21L;

        // when
        redisSessionStore.addUserToRoom(roomId, userId);
        redisSessionStore.addUserToRoom(roomId, userId);
        redisSessionStore.addUserToRoom(roomId, userId);

        Set<Long> roomUsers = redisSessionStore.getRoomUsers(roomId);

        // then
        assertThat(roomUsers).containsExactly(userId);
        assertThat(roomUsers).hasSize(1);
    }

    @Test
    @DisplayName("세션 TTL이 설정되는지 확인")
    void t19() {
        // given
        Long userId = 22L;
        String username = "ttluser";
        String sessionId = "session-ttl-check";
        WebSocketSessionInfo sessionInfo = WebSocketSessionInfo.createNewSession(userId, username, sessionId);

        // when
        redisSessionStore.saveUserSession(userId, sessionInfo);
        String userKey = WebSocketConstants.buildUserSessionKey(userId);
        Long ttl = redisTemplate.getExpire(userKey);

        // then
        assertThat(ttl).isNotNull();
        assertThat(ttl).isGreaterThan(0);
        assertThat(ttl).isLessThanOrEqualTo(WebSocketConstants.SESSION_TTL.getSeconds());
    }

    @Test
    @DisplayName("방 사용자 키에 TTL이 설정되는지 확인")
    void t20() {
        // given
        Long roomId = 105L;
        Long userId = 23L;

        // when
        redisSessionStore.addUserToRoom(roomId, userId);
        String roomUsersKey = WebSocketConstants.buildRoomUsersKey(roomId);
        Long ttl = redisTemplate.getExpire(roomUsersKey);

        // then
        assertThat(ttl).isNotNull();
        assertThat(ttl).isGreaterThan(0);
        assertThat(ttl).isLessThanOrEqualTo(WebSocketConstants.SESSION_TTL.getSeconds());
    }

    @Test
    @DisplayName("Integer 타입 userId도 Long으로 변환하여 조회 가능")
    void t21() {
        // given
        String sessionId = "session-integer-test";

        // Redis에 Integer로 저장 (실제로는 RedisTemplate이 변환할 수 있음)
        String sessionKey = WebSocketConstants.buildSessionUserKey(sessionId);
        redisTemplate.opsForValue().set(sessionKey, 24, WebSocketConstants.SESSION_TTL);

        // when
        Long retrievedUserId = redisSessionStore.getUserIdBySession(sessionId);

        // then
        assertThat(retrievedUserId).isEqualTo(24L);
    }
}