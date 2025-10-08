package com.back.global.websocket.service;

import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import com.back.global.websocket.dto.WebSocketSessionInfo;
import com.back.global.websocket.store.RedisSessionStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 방 참가자 관리 서비스
 * - 방 입장/퇴장 처리
 * - 방별 참가자 목록 관리
 * - 방별 온라인 사용자 통계
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoomParticipantService {

    private final RedisSessionStore redisSessionStore;

    // 사용자 방 입장
    public void enterRoom(Long userId, Long roomId) {
        WebSocketSessionInfo sessionInfo = redisSessionStore.getUserSession(userId);

        if (sessionInfo == null) {
            log.warn("세션 정보가 없어 방 입장 실패 - 사용자: {}, 방: {}", userId, roomId);
            throw new CustomException(ErrorCode.WS_SESSION_NOT_FOUND);
        }

        if (sessionInfo.currentRoomId() != null) {
            exitRoom(userId, sessionInfo.currentRoomId());
            log.debug("기존 방에서 퇴장 처리 완료 - 사용자: {}, 이전 방: {}",
                    userId, sessionInfo.currentRoomId());
        }

        WebSocketSessionInfo updatedSession = sessionInfo.withRoomId(roomId);
        redisSessionStore.saveUserSession(userId, updatedSession);
        redisSessionStore.addUserToRoom(roomId, userId);

        log.info("방 입장 완료 - 사용자: {}, 방: {}", userId, roomId);
    }

    // 사용자 방 퇴장
    public void exitRoom(Long userId, Long roomId) {
        WebSocketSessionInfo sessionInfo = redisSessionStore.getUserSession(userId);

        if (sessionInfo == null) {
            log.warn("세션 정보가 없지만 방 퇴장 처리 계속 진행 - 사용자: {}, 방: {}", userId, roomId);
        } else {
            WebSocketSessionInfo updatedSession = sessionInfo.withoutRoom();
            redisSessionStore.saveUserSession(userId, updatedSession);
        }

        redisSessionStore.removeUserFromRoom(roomId, userId);
        log.info("방 퇴장 완료 - 사용자: {}, 방: {}", userId, roomId);
    }

    // 사용자의 현재 방 ID 조회
    public Long getCurrentRoomId(Long userId) {
        WebSocketSessionInfo sessionInfo = redisSessionStore.getUserSession(userId);
        return sessionInfo != null ? sessionInfo.currentRoomId() : null;
    }

    // 방의 온라인 참가자 목록 조회
    public Set<Long> getParticipants(Long roomId) {
        return redisSessionStore.getRoomUsers(roomId);
    }

    // 방의 온라인 참가자 수 조회
    public long getParticipantCount(Long roomId) {
        return redisSessionStore.getRoomUserCount(roomId);
    }

    // 사용자가 특정 방에 참여 중인지 확인
    public boolean isUserInRoom(Long userId, Long roomId) {
        Long currentRoomId = getCurrentRoomId(userId);
        return currentRoomId != null && currentRoomId.equals(roomId);
    }

    // 모든 방에서 사용자 퇴장 처리 (세션 종료 시 사용)
    public void exitAllRooms(Long userId) {
        try {
            Long currentRoomId = getCurrentRoomId(userId);

            if (currentRoomId != null) {
                exitRoom(userId, currentRoomId);
                log.info("모든 방에서 퇴장 처리 완료 - 사용자: {}", userId);
            }

        } catch (Exception e) {
            log.error("모든 방 퇴장 처리 실패 - 사용자: {}", userId, e);
            // 에러를 던지지 않고 로그만 남김 (세션 종료는 계속 진행되어야 함)
        }
    }

    /**
     * 여러 방의 온라인 참가자 수를 일괄 조회
     * N+1 문제 해결을 위한 일괄 조회 메서드
     * @param roomIds 방 ID 목록
     * @return 방 ID → 참가자 수 맵
     */
    public java.util.Map<Long, Long> getParticipantCounts(java.util.List<Long> roomIds) {
        return redisSessionStore.getRoomUserCounts(roomIds);
    }
}
