package com.back.global.websocket.service;

import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import com.back.global.websocket.dto.WebSocketSessionInfo;
import com.back.global.websocket.store.RedisSessionStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 사용자 세션 관리 서비스
 * - 세션 생명주기 관리 (등록, 종료)
 * - Heartbeat 처리
 * - 중복 연결 방지
 * - 연결 상태 조회
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserSessionService {

    private final RedisSessionStore redisSessionStore;

    // 세션 등록
    public void registerSession(Long userId, String sessionId) {
        WebSocketSessionInfo existingSession = redisSessionStore.getUserSession(userId);
        if (existingSession != null) {
            terminateSession(existingSession.sessionId());
            log.info("기존 세션 제거 후 새 세션 등록 - 사용자: {}", userId);
        }

        WebSocketSessionInfo newSession = WebSocketSessionInfo.createNewSession(userId, sessionId);
        redisSessionStore.saveUserSession(userId, newSession);
        redisSessionStore.saveSessionUserMapping(sessionId, userId);

        redisSessionStore.incrementOnlineUserCount();

        log.info("WebSocket 세션 등록 완료 - 사용자: {}, 세션: {}", userId, sessionId);
    }

    // 세션 종료
    public void terminateSession(String sessionId) {
        Long userId = redisSessionStore.getUserIdBySession(sessionId);

        if (userId != null) {
            redisSessionStore.deleteUserSession(userId);
            redisSessionStore.deleteSessionUserMapping(sessionId);

            redisSessionStore.decrementOnlineUserCount();

            log.info("WebSocket 세션 종료 완료 - 세션: {}, 사용자: {}", sessionId, userId);
        } else {
            log.warn("종료할 세션을 찾을 수 없음 - 세션: {}", sessionId);
        }
    }

    // Heartbeat 처리 (활동 시간 업데이트 및 TTL 연장)
    public void processHeartbeat(Long userId) {
        WebSocketSessionInfo sessionInfo = redisSessionStore.getUserSession(userId);

        if (sessionInfo == null) {
            log.warn("세션 정보가 없어 Heartbeat 처리 실패 - 사용자: {}", userId);
            return;
        }

        WebSocketSessionInfo updatedSession = sessionInfo.withUpdatedActivity();
        redisSessionStore.saveUserSession(userId, updatedSession);

        log.debug("Heartbeat 처리 완료 - 사용자: {}, TTL 연장", userId);
    }

    // 사용자 연결 상태 확인
    public boolean isConnected(Long userId) {
        return redisSessionStore.existsUserSession(userId);
    }

    // 사용자 세션 정보 조회
    public WebSocketSessionInfo getSessionInfo(Long userId) {
        WebSocketSessionInfo sessionInfo = redisSessionStore.getUserSession(userId);

        if (sessionInfo == null) {
            log.debug("세션 정보 없음 - 사용자: {}", userId);
        }

        return sessionInfo;
    }

    // 세션ID로 사용자ID 조회
    public Long getUserIdBySessionId(String sessionId) {
        return redisSessionStore.getUserIdBySession(sessionId);
    }

    // 사용자의 현재 방 ID 조회
    public Long getCurrentRoomId(Long userId) {
        WebSocketSessionInfo sessionInfo = redisSessionStore.getUserSession(userId);
        return sessionInfo != null ? sessionInfo.currentRoomId() : null;
    }

    // 전체 온라인 사용자 수 조회
    public long getTotalOnlineUserCount() {
        return redisSessionStore.getTotalOnlineUserCount();
    }
}