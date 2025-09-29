package com.back.global.websocket.service;

import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import com.back.global.websocket.dto.WebSocketSessionInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * WebSocket 세션 관리 전용 서비스
 * - Redis 기반 세션 상태 관리
 * - 방 입장/퇴장 처리
 * - 온라인 사용자 조회
 * - 브로드캐스트는 WebSocketBroadcastService로 분리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketSessionManager {

    private final RedisTemplate<String, Object> redisTemplate;

    // Redis Key 패턴
    private static final String USER_SESSION_KEY = "ws:user:{}";
    private static final String SESSION_USER_KEY = "ws:session:{}";
    private static final String ROOM_USERS_KEY = "ws:room:{}:users";

    // TTL 설정 (10분)
    private static final int SESSION_TTL_MINUTES = 10;

    // 사용자 세션 추가 (연결 시 호출)
    public void addSession(Long userId, String sessionId) {
        try {
            WebSocketSessionInfo sessionInfo = WebSocketSessionInfo.createNewSession(userId, sessionId);

            String userKey = USER_SESSION_KEY.replace("{}", userId.toString());
            String sessionKey = SESSION_USER_KEY.replace("{}", sessionId);

            // 기존 세션이 있다면 제거 (중복 연결 방지)
            WebSocketSessionInfo existingSession = getSessionInfo(userId);
            if (existingSession != null) {
                removeSessionInternal(existingSession.sessionId());
                log.info("기존 세션 제거 후 새 세션 등록 - 사용자: {}", userId);
            }

            // 새 세션 등록 (TTL 10분)
            redisTemplate.opsForValue().set(userKey, sessionInfo, Duration.ofMinutes(SESSION_TTL_MINUTES));
            redisTemplate.opsForValue().set(sessionKey, userId, Duration.ofMinutes(SESSION_TTL_MINUTES));

            log.info("WebSocket 세션 등록 완료 - 사용자: {}, 세션: {}, TTL: {}분",
                    userId, sessionId, SESSION_TTL_MINUTES);

        } catch (Exception e) {
            log.error("WebSocket 세션 등록 실패 - 사용자: {}", userId, e);
            throw new CustomException(ErrorCode.WS_CONNECTION_FAILED);
        }
    }

    // 사용자 연결 상태 확인
    public boolean isUserConnected(Long userId) {
        try {
            String userKey = USER_SESSION_KEY.replace("{}", userId.toString());
            return Boolean.TRUE.equals(redisTemplate.hasKey(userKey));
        } catch (Exception e) {
            log.error("사용자 연결 상태 확인 실패 - 사용자: {}", userId, e);
            throw new CustomException(ErrorCode.WS_REDIS_ERROR);
        }
    }

    // 사용자 세션 정보 조회
    public WebSocketSessionInfo getSessionInfo(Long userId) {
        try {
            String userKey = USER_SESSION_KEY.replace("{}", userId.toString());
            Object value = redisTemplate.opsForValue().get(userKey);

            if (value == null) {
                return null;
            }

            // LinkedHashMap으로 역직렬화된 경우 또는 타입이 맞지 않는 경우 변환
            if (value instanceof LinkedHashMap || !(value instanceof WebSocketSessionInfo)) {
                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new JavaTimeModule());
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                return mapper.convertValue(value, WebSocketSessionInfo.class);
            }

            return (WebSocketSessionInfo) value;

        } catch (Exception e) {
            log.error("세션 정보 조회 실패 - 사용자: {}", userId, e);
            throw new CustomException(ErrorCode.WS_REDIS_ERROR);
        }
    }

    // 세션 제거 (연결 종료 시 호출)
    public void removeSession(String sessionId) {
        try {
            removeSessionInternal(sessionId);
            log.info("WebSocket 세션 제거 완료 - 세션: {}", sessionId);
        } catch (Exception e) {
            log.error("WebSocket 세션 제거 실패 - 세션: {}", sessionId, e);
            throw new CustomException(ErrorCode.WS_REDIS_ERROR);
        }
    }

    // 사용자 활동 시간 업데이트 및 TTL 연장 (Heartbeat 시 호출)
    public void updateLastActivity(Long userId) {
        try {
            WebSocketSessionInfo sessionInfo = getSessionInfo(userId);
            if (sessionInfo != null) {
                // 마지막 활동 시간 업데이트
                WebSocketSessionInfo updatedSessionInfo = sessionInfo.withUpdatedActivity();

                String userKey = USER_SESSION_KEY.replace("{}", userId.toString());

                // TTL 10분으로 연장
                redisTemplate.opsForValue().set(userKey, updatedSessionInfo, Duration.ofMinutes(SESSION_TTL_MINUTES));

                log.debug("사용자 활동 시간 업데이트 완료 - 사용자: {}, TTL 연장", userId);
            } else {
                log.warn("세션 정보가 없어 활동 시간 업데이트 실패 - 사용자: {}", userId);
            }
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("사용자 활동 시간 업데이트 실패 - 사용자: {}", userId, e);
            throw new CustomException(ErrorCode.WS_ACTIVITY_UPDATE_FAILED);
        }
    }

    // 사용자가 방에 입장 (WebSocket 전용)
    public void joinRoom(Long userId, Long roomId) {
        try {
            WebSocketSessionInfo sessionInfo = getSessionInfo(userId);
            if (sessionInfo != null) {
                // 기존 방에서 퇴장
                if (sessionInfo.currentRoomId() != null) {
                    leaveRoom(userId, sessionInfo.currentRoomId());
                }

                // 새 방 정보 업데이트
                WebSocketSessionInfo updatedSessionInfo = sessionInfo.withRoomId(roomId);

                String userKey = USER_SESSION_KEY.replace("{}", userId.toString());
                redisTemplate.opsForValue().set(userKey, updatedSessionInfo, Duration.ofMinutes(SESSION_TTL_MINUTES));

                // 방 참여자 목록에 추가
                String roomUsersKey = ROOM_USERS_KEY.replace("{}", roomId.toString());
                redisTemplate.opsForSet().add(roomUsersKey, userId);
                redisTemplate.expire(roomUsersKey, Duration.ofMinutes(SESSION_TTL_MINUTES));

                log.info("WebSocket 방 입장 완료 - 사용자: {}, 방: {}", userId, roomId);
            } else {
                log.warn("세션 정보가 없어 방 입장 처리 실패 - 사용자: {}, 방: {}", userId, roomId);
            }
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("사용자 방 입장 실패 - 사용자: {}, 방: {}", userId, roomId, e);
            throw new CustomException(ErrorCode.WS_ROOM_JOIN_FAILED);
        }
    }

    // 사용자가 방에서 퇴장 (WebSocket 전용)
    public void leaveRoom(Long userId, Long roomId) {
        try {
            WebSocketSessionInfo sessionInfo = getSessionInfo(userId);
            if (sessionInfo != null) {
                // 방 정보 제거
                WebSocketSessionInfo updatedSessionInfo = sessionInfo.withoutRoom();

                String userKey = USER_SESSION_KEY.replace("{}", userId.toString());
                redisTemplate.opsForValue().set(userKey, updatedSessionInfo, Duration.ofMinutes(SESSION_TTL_MINUTES));

                // 방 참여자 목록에서 제거
                String roomUsersKey = ROOM_USERS_KEY.replace("{}", roomId.toString());
                redisTemplate.opsForSet().remove(roomUsersKey, userId);

                log.info("WebSocket 방 퇴장 완료 - 사용자: {}, 방: {}", userId, roomId);
            }
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("사용자 방 퇴장 실패 - 사용자: {}, 방: {}", userId, roomId, e);
            throw new CustomException(ErrorCode.WS_ROOM_LEAVE_FAILED);
        }
    }

    // 방의 온라인 사용자 수 조회
    public long getRoomOnlineUserCount(Long roomId) {
        try {
            String roomUsersKey = ROOM_USERS_KEY.replace("{}", roomId.toString());
            Long count = redisTemplate.opsForSet().size(roomUsersKey);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("방 온라인 사용자 수 조회 실패 - 방: {}", roomId, e);
            throw new CustomException(ErrorCode.WS_REDIS_ERROR);
        }
    }

    // 방의 온라인 사용자 목록 조회
    public Set<Long> getOnlineUsersInRoom(Long roomId) {
        try {
            String roomUsersKey = ROOM_USERS_KEY.replace("{}", roomId.toString());
            Set<Object> userIds = redisTemplate.opsForSet().members(roomUsersKey);

            if (userIds != null) {
                return userIds.stream()
                        .map(this::convertToLong)  // 안전한 변환
                        .collect(Collectors.toSet());
            }
            return Set.of();
        } catch (Exception e) {
            log.error("방 온라인 사용자 목록 조회 실패 - 방: {}", roomId, e);
            throw new CustomException(ErrorCode.WS_REDIS_ERROR);
        }
    }

    // 전체 온라인 사용자 수 조회
    public long getTotalOnlineUserCount() {
        try {
            Set<String> userKeys = redisTemplate.keys(USER_SESSION_KEY.replace("{}", "*"));
            return userKeys != null ? userKeys.size() : 0;
        } catch (Exception e) {
            log.error("전체 온라인 사용자 수 조회 실패", e);
            return 0;
        }
    }

    // 특정 사용자의 현재 방 조회
    public Long getUserCurrentRoomId(Long userId) {
        try {
            WebSocketSessionInfo sessionInfo = getSessionInfo(userId);
            return sessionInfo != null ? sessionInfo.currentRoomId() : null;
        } catch (CustomException e) {
            log.error("사용자 현재 방 조회 실패 - 사용자: {}", userId, e);
            return null;
        }
    }

    // 내부적으로 세션 제거 처리
    private void removeSessionInternal(String sessionId) {
        String sessionKey = SESSION_USER_KEY.replace("{}", sessionId);
        Object userIdObj = redisTemplate.opsForValue().get(sessionKey);

        if (userIdObj != null) {
            Long userId = convertToLong(userIdObj);  // 안전한 변환
            WebSocketSessionInfo sessionInfo = getSessionInfo(userId);

            // 방에서 퇴장 처리
            if (sessionInfo != null && sessionInfo.currentRoomId() != null) {
                leaveRoom(userId, sessionInfo.currentRoomId());
            }

            // 세션 데이터 삭제
            String userKey = USER_SESSION_KEY.replace("{}", userId.toString());
            redisTemplate.delete(userKey);
            redisTemplate.delete(sessionKey);
        }
    }

    // Object를 Long으로 안전하게 변환하는 헬퍼 메서드
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