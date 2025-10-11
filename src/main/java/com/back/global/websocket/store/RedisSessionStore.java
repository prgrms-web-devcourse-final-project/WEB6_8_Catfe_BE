package com.back.global.websocket.store;

import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import com.back.global.websocket.config.WebSocketConstants;
import com.back.global.websocket.dto.WebSocketSessionInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

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
public class RedisSessionStore {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisSessionStore(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public void saveUserSession(Long userId, WebSocketSessionInfo sessionInfo) {
        try {
            String userKey = WebSocketConstants.buildUserSessionKey(userId);
            redisTemplate.opsForValue().set(userKey, sessionInfo, WebSocketConstants.SESSION_TTL);
            log.debug("사용자 세션 정보 저장 완료 - userId: {}", userId);
        } catch (Exception e) {
            log.error("사용자 세션 정보 저장 실패 - userId: {}", userId, e);
            throw new CustomException(ErrorCode.WS_REDIS_ERROR);
        }
    }

    public void saveSessionUserMapping(String sessionId, Long userId) {
        try {
            String sessionKey = WebSocketConstants.buildSessionUserKey(sessionId);
            redisTemplate.opsForValue().set(sessionKey, userId, WebSocketConstants.SESSION_TTL);
            log.debug("세션-사용자 매핑 저장 완료 - sessionId: {}", sessionId);
        } catch (Exception e) {
            log.error("세션-사용자 매핑 저장 실패 - sessionId: {}", sessionId, e);
            throw new CustomException(ErrorCode.WS_REDIS_ERROR);
        }
    }

    public WebSocketSessionInfo getUserSession(Long userId) {
        try {
            String userKey = WebSocketConstants.buildUserSessionKey(userId);
            Object value = redisTemplate.opsForValue().get(userKey);

            if (value == null) {
                return null;
            }

            if (value instanceof LinkedHashMap || !(value instanceof WebSocketSessionInfo)) {
                return objectMapper.convertValue(value, WebSocketSessionInfo.class);
            }

            return (WebSocketSessionInfo) value;

        } catch (Exception e) {
            log.error("사용자 세션 정보 조회 실패 - userId: {}", userId, e);
            throw new CustomException(ErrorCode.WS_REDIS_ERROR);
        }
    }

    public Long getUserIdBySession(String sessionId) {
        try {
            String sessionKey = WebSocketConstants.buildSessionUserKey(sessionId);
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

    public void deleteUserSession(Long userId) {
        try {
            String userKey = WebSocketConstants.buildUserSessionKey(userId);
            redisTemplate.delete(userKey);
            log.debug("사용자 세션 정보 삭제 완료 - userId: {}", userId);
        } catch (Exception e) {
            log.error("사용자 세션 정보 삭제 실패 - userId: {}", userId, e);
            throw new CustomException(ErrorCode.WS_REDIS_ERROR);
        }
    }

    public void deleteSessionUserMapping(String sessionId) {
        try {
            String sessionKey = WebSocketConstants.buildSessionUserKey(sessionId);
            redisTemplate.delete(sessionKey);
            log.debug("세션-사용자 매핑 삭제 완료 - sessionId: {}", sessionId);
        } catch (Exception e) {
            log.error("세션-사용자 매핑 삭제 실패 - sessionId: {}", sessionId, e);
            throw new CustomException(ErrorCode.WS_REDIS_ERROR);
        }
    }

    public boolean existsUserSession(Long userId) {
        try {
            String userKey = WebSocketConstants.buildUserSessionKey(userId);
            return Boolean.TRUE.equals(redisTemplate.hasKey(userKey));
        } catch (Exception e) {
            log.error("사용자 세션 존재 여부 확인 실패 - userId: {}", userId, e);
            throw new CustomException(ErrorCode.WS_REDIS_ERROR);
        }
    }

    public void addUserToRoom(Long roomId, Long userId) {
        try {
            String roomUsersKey = WebSocketConstants.buildRoomUsersKey(roomId);
            redisTemplate.opsForSet().add(roomUsersKey, userId);
            redisTemplate.expire(roomUsersKey, WebSocketConstants.SESSION_TTL);
            log.debug("방에 사용자 추가 완료 - roomId: {}, userId: {}", roomId, userId);
        } catch (Exception e) {
            log.error("방에 사용자 추가 실패 - roomId: {}, userId: {}", roomId, userId, e);
            throw new CustomException(ErrorCode.WS_REDIS_ERROR);
        }
    }

    public void removeUserFromRoom(Long roomId, Long userId) {
        try {
            String roomUsersKey = WebSocketConstants.buildRoomUsersKey(roomId);
            redisTemplate.opsForSet().remove(roomUsersKey, userId);
            log.debug("방에서 사용자 제거 완료 - roomId: {}, userId: {}", roomId, userId);
        } catch (Exception e) {
            log.error("방에서 사용자 제거 실패 - roomId: {}, userId: {}", roomId, userId, e);
            throw new CustomException(ErrorCode.WS_REDIS_ERROR);
        }
    }

    public Set<Long> getRoomUsers(Long roomId) {
        try {
            String roomUsersKey = WebSocketConstants.buildRoomUsersKey(roomId);
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

    public long getRoomUserCount(Long roomId) {
        try {
            String roomUsersKey = WebSocketConstants.buildRoomUsersKey(roomId);
            Long count = redisTemplate.opsForSet().size(roomUsersKey);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("방 사용자 수 조회 실패 - roomId: {}", roomId, e);
            throw new CustomException(ErrorCode.WS_REDIS_ERROR);
        }
    }

    public long getTotalOnlineUserCount() {
        try {
            // 카운터 키에서 직접 값을 가져옴
            Object count = redisTemplate.opsForValue().get(WebSocketConstants.ONLINE_USER_COUNT_KEY);
            if (count instanceof Number) {
                return ((Number) count).longValue();
            }
            return 0L;
        } catch (Exception e) {
            log.error("전체 온라인 사용자 수 조회 실패", e);
            return 0; // 에러 발생 시 0 반환
        }
    }

    public void incrementOnlineUserCount() {
        try {
            redisTemplate.opsForValue().increment(WebSocketConstants.ONLINE_USER_COUNT_KEY);
        } catch (Exception e) {
            log.error("온라인 사용자 수 증가 실패", e);
        }
    }

    public void decrementOnlineUserCount() {
        try {
            // 카운터가 0보다 작아지지 않도록 방지
            Long currentValue = redisTemplate.opsForValue().decrement(WebSocketConstants.ONLINE_USER_COUNT_KEY);
            if (currentValue != null && currentValue < 0) {
                redisTemplate.opsForValue().set(WebSocketConstants.ONLINE_USER_COUNT_KEY, 0L);
            }
        } catch (Exception e) {
            log.error("온라인 사용자 수 감소 실패", e);
        }
    }

    /**
     * 여러 방의 사용자 수를 일괄 조회 (Redis Pipeline 사용)
     * N+1 문제 해결을 위한 일괄 조회 메서드
     * @param roomIds 조회할 방 ID 목록
     * @return 방 ID → 사용자 수 맵
     */
    public java.util.Map<Long, Long> getRoomUserCounts(java.util.List<Long> roomIds) {
        if (roomIds == null || roomIds.isEmpty()) {
            return java.util.Map.of();
        }

        try {
            // Pipeline을 사용하여 한 번에 여러 SET 크기 조회
            java.util.List<Object> results = redisTemplate.executePipelined(
                (org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
                    for (Long roomId : roomIds) {
                        String roomUsersKey = WebSocketConstants.buildRoomUsersKey(roomId);
                        connection.setCommands().sCard(roomUsersKey.getBytes());
                    }
                    return null;
                }
            );

            // 결과를 Map으로 변환
            java.util.Map<Long, Long> resultMap = new java.util.HashMap<>();
            for (int i = 0; i < roomIds.size(); i++) {
                Long count = results.get(i) != null ? ((Number) results.get(i)).longValue() : 0L;
                resultMap.put(roomIds.get(i), count);
            }

            log.debug("방 사용자 수 일괄 조회 완료 - 방 개수: {}", roomIds.size());
            return resultMap;

        } catch (Exception e) {
            log.error("방 사용자 수 일괄 조회 실패 - 방 개수: {}", roomIds.size(), e);
            // 에러 시 개별 조회로 폴백
            return roomIds.stream()
                    .collect(java.util.stream.Collectors.toMap(
                            roomId -> roomId,
                            this::getRoomUserCount
                    ));
        }
    }

    private Long convertToLong(Object obj) {
        if (obj instanceof Long) {
            return (Long) obj;
        } else if (obj instanceof Number) {
            return ((Number) obj).longValue();
        } else {
            throw new IllegalArgumentException("Cannot convert " + obj.getClass() + " to Long");
        }
    }
    
    // ==================== 범용 Key-Value 저장/조회 메서드 ====================
    
    /**
     * 범용 값 저장 (TTL 포함)
     * @param key Redis Key
     * @param value 저장할 값
     * @param ttl TTL (Duration)
     */
    public void saveValue(String key, String value, java.time.Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
            log.debug("값 저장 완료 - Key: {}, TTL: {}분", key, ttl.toMinutes());
        } catch (Exception e) {
            log.error("값 저장 실패 - Key: {}", key, e);
            throw new CustomException(ErrorCode.WS_REDIS_ERROR);
        }
    }
    
    /**
     * 범용 값 조회
     * @param key Redis Key
     * @return 저장된 값 (없으면 null)
     */
    public String getValue(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            log.error("값 조회 실패 - Key: {}", key, e);
            return null; // 에러 시 null 반환 (예외 던지지 않음)
        }
    }
    
    /**
     * 범용 값 삭제
     * @param key Redis Key
     */
    public void deleteValue(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("값 삭제 완료 - Key: {}", key);
        } catch (Exception e) {
            log.error("값 삭제 실패 - Key: {}", key, e);
        }
    }
}