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

    private final UserSessionService userSessionService;
    private final RedisSessionStore redisSessionStore;

    // 사용자 세션 추가 (연결 시 호출)
    public void addSession(Long userId, String sessionId) {
        userSessionService.registerSession(userId, sessionId);
    }

    // 세션 제거 (연결 종료 시 호출)
    public void removeSession(String sessionId) {
        try {
            Long userId = userSessionService.getUserIdBySessionId(sessionId);

            if (userId != null) {
                // 방에서 퇴장 처리
                Long currentRoomId = userSessionService.getCurrentRoomId(userId);
                if (currentRoomId != null) {
                    leaveRoom(userId, currentRoomId);
                }
            }

            // 세션 종료
            userSessionService.terminateSession(sessionId);

        } catch (Exception e) {
            log.error("WebSocket 세션 제거 실패 - 세션: {}", sessionId, e);
            throw new CustomException(ErrorCode.WS_REDIS_ERROR);
        }
    }

    // 사용자 연결 상태 확인
    public boolean isUserConnected(Long userId) {
        return userSessionService.isConnected(userId);
    }

    // 사용자 세션 정보 조회
    public WebSocketSessionInfo getSessionInfo(Long userId) {
        return userSessionService.getSessionInfo(userId);
    }

    // 사용자 활동 시간 업데이트 (Heartbeat 시 호출)
    public void updateLastActivity(Long userId) {
        userSessionService.processHeartbeat(userId);
    }

    // 전체 온라인 사용자 수 조회
    public long getTotalOnlineUserCount() {
        return userSessionService.getTotalOnlineUserCount();
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

    // 특정 사용자의 현재 방 조회
    public Long getUserCurrentRoomId(Long userId) {
        return userSessionService.getCurrentRoomId(userId);
    }
}