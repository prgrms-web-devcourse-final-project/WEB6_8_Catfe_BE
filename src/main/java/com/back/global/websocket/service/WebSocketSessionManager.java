package com.back.global.websocket.service;

import com.back.domain.studyroom.dto.RoomBroadcastMessage;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import com.back.global.websocket.dto.WebSocketSessionInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketSessionManager {

    private final RedisTemplate<String, Object> redisTemplate;

    // 브로드캐스트 부분에서 순환 참조가 생겨서 Lazy 주입
    @Lazy
    private final SimpMessagingTemplate messagingTemplate;

    // Redis Key 패턴
    private static final String USER_SESSION_KEY = "ws:user:{}";
    private static final String SESSION_USER_KEY = "ws:session:{}";
    private static final String ROOM_USERS_KEY = "ws:room:{}:users";

    // TTL 설정 (10분) - Heartbeat와 함께 사용하여 정확한 상태 관리
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
            return (WebSocketSessionInfo) redisTemplate.opsForValue().get(userKey);
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
            // 이미 처리된 CustomException은 다시 던짐
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
                        .map(obj -> (Long) obj)
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
            return null; // 조회용이므로 예외 대신 null 반환
        }
    }

    // 내부적으로 세션 제거 처리
    private void removeSessionInternal(String sessionId) {
        String sessionKey = SESSION_USER_KEY.replace("{}", sessionId);
        Long userId = (Long) redisTemplate.opsForValue().get(sessionKey);

        if (userId != null) {
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

    // ======================== 브로드캐스트 기능 ========================

    /**
     * 특정 방의 모든 온라인 사용자에게 메시지 브로드캐스트
     */
    public void broadcastToRoom(Long roomId, RoomBroadcastMessage message) {
        try {
            Set<Long> onlineUsers = getOnlineUsersInRoom(roomId);

            if (onlineUsers.isEmpty()) {
                log.debug("브로드캐스트 대상이 없음 - 방: {}", roomId);
                return;
            }

            // 방 전체 토픽으로 브로드캐스트
            String destination = "/topic/rooms/" + roomId + "/updates";
            messagingTemplate.convertAndSend(destination, message);

            log.info("방 브로드캐스트 완료 - 방: {}, 타입: {}, 대상: {}명",
                    roomId, message.getType(), onlineUsers.size());

        } catch (Exception e) {
            log.error("방 브로드캐스트 실패 - 방: {}, 타입: {}", roomId, message.getType(), e);
            throw new CustomException(ErrorCode.WS_REDIS_ERROR);
        }
    }

    /**
     * 특정 사용자에게 개인 메시지 전송
     */
    public void sendToUser(Long userId, String destination, Object message) {
        try {
            if (isUserConnected(userId)) {
                messagingTemplate.convertAndSendToUser(
                        userId.toString(),
                        destination,
                        message
                );

                log.debug("개인 메시지 전송 완료 - 사용자: {}, 목적지: {}", userId, destination);
            } else {
                log.debug("오프라인 사용자에게 메시지 전송 시도 - 사용자: {}", userId);
            }
        } catch (Exception e) {
            log.error("개인 메시지 전송 실패 - 사용자: {}", userId, e);
        }
    }

    /**
     * 방의 온라인 멤버 목록 업데이트 브로드캐스트
     */
    public void broadcastOnlineMembersUpdate(Long roomId) {
        try {
            Set<Long> onlineUsers = getOnlineUsersInRoom(roomId);

            // 온라인 사용자 ID 목록을 브로드캐스트
            RoomBroadcastMessage message = RoomBroadcastMessage.onlineMembersUpdated(
                    roomId,
                    onlineUsers.stream().toList()
            );

            broadcastToRoom(roomId, message);

        } catch (Exception e) {
            log.error("온라인 멤버 목록 브로드캐스트 실패 - 방: {}", roomId, e);
        }
    }

    /**
     * 방 상태 변경 알림 브로드캐스트
     */
    public void broadcastRoomUpdate(Long roomId, String updateMessage) {
        try {
            RoomBroadcastMessage message = RoomBroadcastMessage.roomUpdated(roomId, updateMessage);
            broadcastToRoom(roomId, message);
        } catch (Exception e) {
            log.error("방 상태 변경 브로드캐스트 실패 - 방: {}", roomId, e);
        }
    }
}