package com.back.domain.studyroom.service;

import com.back.global.websocket.service.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

/**
 * 방 상태 관리를 위한 Redis 전용 서비스
 * 
 * @deprecated RoomParticipantService를 사용하세요.
 *             이 서비스는 WebSocketSessionManager의 Wrapper일 뿐이며,
 *             RoomParticipantService가 더 직접적이고 명확합니다.
 * 
 * @see com.back.global.websocket.service.RoomParticipantService 실제 사용 서비스
 * @see com.back.global.websocket.service.WebSocketSessionManager WebSocket 세션 관리
 */
@Deprecated
@Slf4j
@Service
@RequiredArgsConstructor
public class RoomRedisService {

    private final WebSocketSessionManager sessionManager;

    // ==================== 방 입장/퇴장 ====================

    /**
     * 사용자가 방에 입장 (Redis 온라인 상태 업데이트)
     * - Redis Set에 userId 추가
     * - 역할(Role)은 DB에서만 관리
     *
     * @param userId 사용자 ID
     * @param roomId 방 ID
     */
    public void enterRoom(Long userId, Long roomId) {
        sessionManager.joinRoom(userId, roomId);
        log.info("방 입장 완료 (Redis) - 사용자: {}, 방: {}", userId, roomId);
    }

    /**
     * 사용자가 방에서 퇴장 (Redis 온라인 상태 업데이트)
     * - Redis Set에서 userId 제거
     * - DB 멤버십은 유지됨 (재입장 시 역할 유지)
     *
     * @param userId 사용자 ID
     * @param roomId 방 ID
     */
    public void exitRoom(Long userId, Long roomId) {
        sessionManager.leaveRoom(userId, roomId);
        log.info("방 퇴장 완료 (Redis) - 사용자: {}, 방: {}", userId, roomId);
    }

    // ==================== 조회 ====================

    /**
     * 방의 현재 온라인 사용자 수 조회
     * - 실시간 참가자 수 (DB currentParticipants와 무관)
     *
     * @param roomId 방 ID
     * @return 온라인 사용자 수
     */
    public long getRoomUserCount(Long roomId) {
        return sessionManager.getRoomOnlineUserCount(roomId);
    }

    /**
     * 방의 온라인 사용자 ID 목록 조회
     * - 멤버 목록 조회 시 이 ID로 DB 조회
     * - DB에 없는 ID = VISITOR
     *
     * @param roomId 방 ID
     * @return 온라인 사용자 ID Set
     */
    public Set<Long> getRoomUsers(Long roomId) {
        return sessionManager.getOnlineUsersInRoom(roomId);
    }

    /**
     * 사용자가 현재 특정 방에 있는지 확인
     *
     * @param userId 사용자 ID
     * @param roomId 방 ID
     * @return 온라인 여부
     */
    public boolean isUserInRoom(Long userId, Long roomId) {
        return sessionManager.isUserInRoom(userId, roomId);
    }

    /**
     * 사용자의 현재 방 ID 조회
     * 
     * @param userId 사용자 ID
     * @return 방 ID (없으면 null)
     */
    public Long getCurrentRoomId(Long userId) {
        return sessionManager.getUserCurrentRoomId(userId);
    }

    /**
     * 여러 방의 온라인 사용자 수 일괄 조회 (N+1 방지)
     * - 방 목록 조회 시 사용
     * 
     * @param roomIds 방 ID 목록
     * @return Map<RoomId, OnlineCount>
     */
    public Map<Long, Long> getBulkRoomOnlineUserCounts(java.util.List<Long> roomIds) {
        return sessionManager.getBulkRoomOnlineUserCounts(roomIds);
    }
}
