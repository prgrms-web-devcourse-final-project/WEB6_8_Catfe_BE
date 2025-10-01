package com.back.global.websocket.service;

import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import com.back.global.websocket.dto.WebSocketSessionInfo;
import com.back.global.websocket.store.RedisSessionStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketSessionManager {

    private final RedisSessionStore redisSessionStore;

    // 사용자 세션 추가 (연결 시 호출)
    public void addSession(Long userId, String sessionId) {
        try {
            WebSocketSessionInfo sessionInfo = WebSocketSessionInfo.createNewSession(userId, sessionId);

            // 기존 세션이 있다면 제거 (중복 연결 방지)
            WebSocketSessionInfo existingSession = redisSessionStore.getUserSession(userId);
            if (existingSession != null) {
                removeSessionInternal(existingSession.sessionId());
                log.info("기존 세션 제거 후 새 세션 등록 - 사용자: {}", userId);
            }

            // 새 세션 등록
            redisSessionStore.saveUserSession(userId, sessionInfo);
            redisSessionStore.saveSessionUserMapping(sessionId, userId);

            log.info("WebSocket 세션 등록 완료 - 사용자: {}, 세션: {}", userId, sessionId);

        } catch (Exception e) {
            log.error("WebSocket 세션 등록 실패 - 사용자: {}", userId, e);
            throw new CustomException(ErrorCode.WS_CONNECTION_FAILED);
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

    // 사용자 연결 상태 확인
    public boolean isUserConnected(Long userId) {
        try {
            return redisSessionStore.existsUserSession(userId);
        } catch (Exception e) {
            log.error("사용자 연결 상태 확인 실패 - 사용자: {}", userId, e);
            throw new CustomException(ErrorCode.WS_REDIS_ERROR);
        }
    }

    // 사용자 세션 정보 조회
    public WebSocketSessionInfo getSessionInfo(Long userId) {
        try {
            return redisSessionStore.getUserSession(userId);
        } catch (Exception e) {
            log.error("세션 정보 조회 실패 - 사용자: {}", userId, e);
            throw new CustomException(ErrorCode.WS_REDIS_ERROR);
        }
    }

    // 사용자 활동 시간 업데이트 및 TTL 연장 (Heartbeat 시 호출)
    public void updateLastActivity(Long userId) {
        try {
            WebSocketSessionInfo sessionInfo = redisSessionStore.getUserSession(userId);
            if (sessionInfo != null) {
                // 마지막 활동 시간 업데이트
                WebSocketSessionInfo updatedSessionInfo = sessionInfo.withUpdatedActivity();

                // TTL 연장하며 저장
                redisSessionStore.saveUserSession(userId, updatedSessionInfo);

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
            WebSocketSessionInfo sessionInfo = redisSessionStore.getUserSession(userId);
            if (sessionInfo != null) {
                // 기존 방에서 퇴장
                if (sessionInfo.currentRoomId() != null) {
                    leaveRoom(userId, sessionInfo.currentRoomId());
                }

                // 새 방 정보 업데이트
                WebSocketSessionInfo updatedSessionInfo = sessionInfo.withRoomId(roomId);
                redisSessionStore.saveUserSession(userId, updatedSessionInfo);

                // 방 참여자 목록에 추가
                redisSessionStore.addUserToRoom(roomId, userId);

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
            WebSocketSessionInfo sessionInfo = redisSessionStore.getUserSession(userId);
            if (sessionInfo != null) {
                // 방 정보 제거
                WebSocketSessionInfo updatedSessionInfo = sessionInfo.withoutRoom();
                redisSessionStore.saveUserSession(userId, updatedSessionInfo);

                // 방 참여자 목록에서 제거
                redisSessionStore.removeUserFromRoom(roomId, userId);

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
            return redisSessionStore.getRoomUserCount(roomId);
        } catch (Exception e) {
            log.error("방 온라인 사용자 수 조회 실패 - 방: {}", roomId, e);
            throw new CustomException(ErrorCode.WS_REDIS_ERROR);
        }
    }

    // 방의 온라인 사용자 목록 조회
    public Set<Long> getOnlineUsersInRoom(Long roomId) {
        try {
            return redisSessionStore.getRoomUsers(roomId);
        } catch (Exception e) {
            log.error("방 온라인 사용자 목록 조회 실패 - 방: {}", roomId, e);
            throw new CustomException(ErrorCode.WS_REDIS_ERROR);
        }
    }

    // 전체 온라인 사용자 수 조회
    public long getTotalOnlineUserCount() {
        try {
            return redisSessionStore.getTotalOnlineUserCount();
        } catch (Exception e) {
            log.error("전체 온라인 사용자 수 조회 실패", e);
            return 0;
        }
    }

    // 특정 사용자의 현재 방 조회
    public Long getUserCurrentRoomId(Long userId) {
        try {
            WebSocketSessionInfo sessionInfo = redisSessionStore.getUserSession(userId);
            return sessionInfo != null ? sessionInfo.currentRoomId() : null;
        } catch (CustomException e) {
            log.error("사용자 현재 방 조회 실패 - 사용자: {}", userId, e);
            return null;
        }
    }

    // 내부적으로 세션 제거 처리
    private void removeSessionInternal(String sessionId) {
        Long userId = redisSessionStore.getUserIdBySession(sessionId);

        if (userId != null) {
            WebSocketSessionInfo sessionInfo = redisSessionStore.getUserSession(userId);

            // 방에서 퇴장 처리
            if (sessionInfo != null && sessionInfo.currentRoomId() != null) {
                leaveRoom(userId, sessionInfo.currentRoomId());
            }

            // 세션 데이터 삭제
            redisSessionStore.deleteUserSession(userId);
            redisSessionStore.deleteSessionUserMapping(sessionId);
        }
    }
}