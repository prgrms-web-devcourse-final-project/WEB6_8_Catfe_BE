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
        try {
            // 기존 세션 확인 및 제거
            WebSocketSessionInfo existingSession = redisSessionStore.getUserSession(userId);
            if (existingSession != null) {
                terminateSession(existingSession.sessionId());
                log.info("기존 세션 제거 후 새 세션 등록 - 사용자: {}", userId);
            }

            // 새 세션 생성 및 등록
            WebSocketSessionInfo newSession = WebSocketSessionInfo.createNewSession(userId, sessionId);
            redisSessionStore.saveUserSession(userId, newSession);
            redisSessionStore.saveSessionUserMapping(sessionId, userId);

            log.info("WebSocket 세션 등록 완료 - 사용자: {}, 세션: {}", userId, sessionId);

        } catch (Exception e) {
            log.error("WebSocket 세션 등록 실패 - 사용자: {}", userId, e);
            throw new CustomException(ErrorCode.WS_CONNECTION_FAILED);
        }
    }

    // 세션 종료
    public void terminateSession(String sessionId) {
        try {
            Long userId = redisSessionStore.getUserIdBySession(sessionId);

            if (userId != null) {
                // 세션 데이터 삭제
                redisSessionStore.deleteUserSession(userId);
                redisSessionStore.deleteSessionUserMapping(sessionId);

                log.info("WebSocket 세션 종료 완료 - 세션: {}, 사용자: {}", sessionId, userId);
            } else {
                log.warn("종료할 세션을 찾을 수 없음 - 세션: {}", sessionId);
            }

        } catch (Exception e) {
            log.error("WebSocket 세션 종료 실패 - 세션: {}", sessionId, e);
            throw new CustomException(ErrorCode.WS_REDIS_ERROR);
        }
    }

    // Heartbeat 처리 (활동 시간 업데이트 및 TTL 연장)
    public void processHeartbeat(Long userId) {
        try {
            WebSocketSessionInfo sessionInfo = redisSessionStore.getUserSession(userId);

            if (sessionInfo == null) {
                log.warn("세션 정보가 없어 Heartbeat 처리 실패 - 사용자: {}", userId);
                return;
            }

            // 마지막 활동 시간 업데이트
            WebSocketSessionInfo updatedSession = sessionInfo.withUpdatedActivity();

            // TTL 연장하며 저장
            redisSessionStore.saveUserSession(userId, updatedSession);

            log.debug("Heartbeat 처리 완료 - 사용자: {}, TTL 연장", userId);

        } catch (Exception e) {
            log.error("Heartbeat 처리 실패 - 사용자: {}", userId, e);
            throw new CustomException(ErrorCode.WS_ACTIVITY_UPDATE_FAILED);
        }
    }

    // 사용자 연결 상태 확인
    public boolean isConnected(Long userId) {
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
            WebSocketSessionInfo sessionInfo = redisSessionStore.getUserSession(userId);

            if (sessionInfo == null) {
                log.debug("세션 정보 없음 - 사용자: {}", userId);
            }

            return sessionInfo;

        } catch (Exception e) {
            log.error("세션 정보 조회 실패 - 사용자: {}", userId, e);
            throw new CustomException(ErrorCode.WS_REDIS_ERROR);
        }
    }

    // 세션ID로 사용자ID 조회
    public Long getUserIdBySessionId(String sessionId) {
        try {
            return redisSessionStore.getUserIdBySession(sessionId);
        } catch (Exception e) {
            log.error("세션으로 사용자 조회 실패 - 세션: {}", sessionId, e);
            throw new CustomException(ErrorCode.WS_REDIS_ERROR);
        }
    }

    // 사용자의 현재 방 ID 조회
    public Long getCurrentRoomId(Long userId) {
        try {
            WebSocketSessionInfo sessionInfo = redisSessionStore.getUserSession(userId);
            return sessionInfo != null ? sessionInfo.currentRoomId() : null;
        } catch (Exception e) {
            log.error("사용자 현재 방 조회 실패 - 사용자: {}", userId, e);
            return null;
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
}