package com.back.global.websocket.store;

import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import com.back.global.websocket.dto.WebSocketSessionInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Redis 저장소 계층
 * - Redis CRUD 연산
 * - Key 패턴 관리
 * - TTL 관리
 * - 타입 변환
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisSessionStore {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    // Redis Key 패턴
    private static final String USER_SESSION_KEY = "ws:user:{}";
    private static final String SESSION_USER_KEY = "ws:session:{}";
    private static final String ROOM_USERS_KEY = "ws:room:{}:users";

    // TTL 설정
    private static final Duration SESSION_TTL = Duration.ofMinutes(6);

    public RedisSessionStore(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    // ============= 세션 정보 저장/조회/삭제 =============

    // 사용자 세션 정보 저장
    public void saveUserSession(Long userId, WebSocketSessionInfo sessionInfo) {
        try {
            String userKey = buildUserSessionKey(userId);
            redisTemplate.opsForValue().set(userKey, sessionInfo, SESSION_TTL);
            log.debug("사용자 세션 정보 저장 완료 - userId: {}", userId);
        } catch (Exception e) {
            log.error("사용자 세션 정보 저장 실패 - userId: {}", userId, e);
            throw new CustomException(ErrorCode.WS_REDIS_ERROR);
        }
    }

    // 세션ID → 사용자ID 매핑 저장
    public void saveSessionUserMapping(String sessionId, Long userId) {
        try {
            String sessionKey = buildSessionUserKey(sessionId);
            redisTemplate.opsForValue().set(sessionKey, userId, SESSION_TTL);
            log.debug("세션-사용자 매핑 저장 완료 - sessionId: {}", sessionId);
        } catch (Exception e) {
            log.error("세션-사용자 매핑 저장 실패 - sessionId: {}", sessionId, e);
            throw new CustomException(ErrorCode.WS_REDIS_ERROR);
        }
    }

    // 사용자 세션 정보 조회
    public WebSocketSessionInfo getUserSession(Long userId) {
        try {
            String userKey = buildUserSessionKey(userId);
            Object value = redisTemplate.opsForValue().get(userKey);

            if (value == null) {
                return null;
            }

            // LinkedHashMap으로 역직렬화된 경우 변환
            if (value instanceof LinkedHashMap || !(value instanceof WebSocketSessionInfo)) {
                return objectMapper.convertValue(value, WebSocketSessionInfo.class);
            }

            return (WebSocketSessionInfo) value;

        } catch (Exception e) {
            log.error("사용자 세션 정보 조회 실패 - userId: {}", userId, e);
            throw new CustomException(ErrorCode.WS_REDIS_ERROR);
        }
    }

    // 세션ID로 사용자ID 조회
    public Long getUserIdBySession(String sessionId) {
        try {
            String sessionKey = buildSessionUserKey(sessionId);
            Object value = redisTemplate.opsForValue().get(sessionKey);

            if (value == null) {
                return null;
            }

            return convertToLong(value);

        } catch (Exception e) {
            log.error("세션으로 사용자 조회 실패 - sessionId: {}", sessionId, e);
            throw new CustomException(ErrorCode.WS_REDIS_ERROR);
        }
    }

    // 사용자 세션 정보 삭제
    public void deleteUserSession(Long userId) {
        try {
            String userKey = buildUserSessionKey(userId);
            redisTemplate.delete(userKey);
            log.debug("사용자 세션 정보 삭제 완료 - userId: {}", userId);
        } catch (Exception e) {
            log.error("사용자 세션 정보 삭제 실패 - userId: {}", userId, e);
            throw new CustomException(ErrorCode.WS_REDIS_ERROR);
        }
    }

    // 세션-사용자 매핑 삭제
    public void deleteSessionUserMapping(String sessionId) {
        try {
            String sessionKey = buildSessionUserKey(sessionId);
            redisTemplate.delete(sessionKey);
            log.debug("세션-사용자 매핑 삭제 완료 - sessionId: {}", sessionId);
        } catch (Exception e) {
            log.error("세션-사용자 매핑 삭제 실패 - sessionId: {}", sessionId, e);
            throw new CustomException(ErrorCode.WS_REDIS_ERROR);
        }
    }

    // 사용자 세션 존재 여부 확인
    public boolean existsUserSession(Long userId) {
        try {
            String userKey = buildUserSessionKey(userId);
            return Boolean.TRUE.equals(redisTemplate.hasKey(userKey));
        } catch (Exception e) {
            log.error("사용자 세션 존재 여부 확인 실패 - userId: {}", userId, e);
            throw new CustomException(ErrorCode.WS_REDIS_ERROR);
        }
    }

    // ============= 방 참가자 관리 =============

    // 방에 사용자 추가
    public void addUserToRoom(Long roomId, Long userId) {
        try {
            String roomUsersKey = buildRoomUsersKey(roomId);
            redisTemplate.opsForSet().add(roomUsersKey, userId);
            redisTemplate.expire(roomUsersKey, SESSION_TTL);
            log.debug("방에 사용자 추가 완료 - roomId: {}, userId: {}", roomId, userId);
        } catch (Exception e) {
            log.error("방에 사용자 추가 실패 - roomId: {}, userId: {}", roomId, userId, e);
            throw new CustomException(ErrorCode.WS_REDIS_ERROR);
        }
    }

    // 방에서 사용자 제거
    public void removeUserFromRoom(Long roomId, Long userId) {
        try {
            String roomUsersKey = buildRoomUsersKey(roomId);
            redisTemplate.opsForSet().remove(roomUsersKey, userId);
            log.debug("방에서 사용자 제거 완료 - roomId: {}, userId: {}", roomId, userId);
        } catch (Exception e) {
            log.error("방에서 사용자 제거 실패 - roomId: {}, userId: {}", roomId, userId, e);
            throw new CustomException(ErrorCode.WS_REDIS_ERROR);
        }
    }

    // 방의 사용자 목록 조회
    public Set<Long> getRoomUsers(Long roomId) {
        try {
            String roomUsersKey = buildRoomUsersKey(roomId);
            Set<Object> userIds = redisTemplate.opsForSet().members(roomUsersKey);

            if (userIds != null) {
                return userIds.stream()
                        .map(this::convertToLong)
                        .collect(Collectors.toSet());
            }
            return Set.of();

        } catch (Exception e) {
            log.error("방 사용자 목록 조회 실패 - roomId: {}", roomId, e);
            throw new CustomException(ErrorCode.WS_REDIS_ERROR);
        }
    }

    // 방의 사용자 수 조회
    public long getRoomUserCount(Long roomId) {
        try {
            String roomUsersKey = buildRoomUsersKey(roomId);
            Long count = redisTemplate.opsForSet().size(roomUsersKey);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("방 사용자 수 조회 실패 - roomId: {}", roomId, e);
            throw new CustomException(ErrorCode.WS_REDIS_ERROR);
        }
    }

    // ============= 전체 통계 =============

    // 전체 온라인 사용자 수 조회
    public long getTotalOnlineUserCount() {
        try {
            Set<String> userKeys = redisTemplate.keys(buildUserSessionKey("*"));
            return userKeys != null ? userKeys.size() : 0;
        } catch (Exception e) {
            log.error("전체 온라인 사용자 수 조회 실패", e);
            return 0;
        }
    }

    // ============= Key 생성 헬퍼 =============

    private String buildUserSessionKey(Object userId) {
        return USER_SESSION_KEY.replace("{}", userId.toString());
    }

    private String buildSessionUserKey(String sessionId) {
        return SESSION_USER_KEY.replace("{}", sessionId);
    }

    private String buildRoomUsersKey(Long roomId) {
        return ROOM_USERS_KEY.replace("{}", roomId.toString());
    }

    // ============= 타입 변환 헬퍼 =============

    // Object를 Long으로 안전하게 변환
    private Long convertToLong(Object obj) {
        if (obj instanceof Long) {
            return (Long) obj;
        } else if (obj instanceof Number) {
            return ((Number) obj).longValue();
        } else {
            throw new IllegalArgumentException("Cannot convert " + obj.getClass() + " to Long");
        }
    }
}