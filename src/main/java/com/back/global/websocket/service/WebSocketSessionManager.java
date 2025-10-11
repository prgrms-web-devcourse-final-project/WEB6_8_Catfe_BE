package com.back.global.websocket.service;

import com.back.global.websocket.dto.WebSocketSessionInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketSessionManager {

    private final UserSessionService userSessionService;
    private final RoomParticipantService roomParticipantService;

    // 사용자 세션 추가 (WebSocket 연결 시 호출)
    public void addSession(Long userId, String username, String sessionId) {
        userSessionService.registerSession(userId, username, sessionId);
    }

    // 세션 제거 (WebSocket 연결 종료 시 호출)
    public void removeSession(String sessionId) {
        Long userId = userSessionService.getUserIdBySessionId(sessionId);

        if (userId != null) {
            // 1. 모든 방에서 퇴장
            roomParticipantService.exitAllRooms(userId);

            // 2. 세션 종료
            userSessionService.terminateSession(sessionId);
        } else {
            log.warn("종료할 세션을 찾을 수 없음 - 세션: {}", sessionId);
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

    // Heartbeat 처리 (활동 시간 업데이트 및 TTL 연장)
    public void updateLastActivity(Long userId) {
        userSessionService.processHeartbeat(userId);
    }

    // 전체 온라인 사용자 수 조회
    public long getTotalOnlineUserCount() {
        return userSessionService.getTotalOnlineUserCount();
    }

    // 사용자가 방에 입장
    public void joinRoom(Long userId, Long roomId) {
        roomParticipantService.enterRoom(userId, roomId);
    }

    // 사용자가 방에서 퇴장
    public void leaveRoom(Long userId, Long roomId) {
        roomParticipantService.exitRoom(userId, roomId);
    }

    // 방의 온라인 사용자 수 조회
    public long getRoomOnlineUserCount(Long roomId) {
        return roomParticipantService.getParticipantCount(roomId);
    }

    // 방의 온라인 사용자 목록 조회
    public Set<Long> getOnlineUsersInRoom(Long roomId) {
        return roomParticipantService.getParticipants(roomId);
    }

    // 특정 사용자의 현재 방 조회
    public Long getUserCurrentRoomId(Long userId) {
        return roomParticipantService.getCurrentRoomId(userId);
    }

    // 사용자가 특정 방에 참여 중인지 확인
    public boolean isUserInRoom(Long userId, Long roomId) {
        return roomParticipantService.isUserInRoom(userId, roomId);
    }

    // 여러 방의 온라인 사용자 수 일괄 조회 (N+1 방지)
    public java.util.Map<Long, Long> getBulkRoomOnlineUserCounts(java.util.List<Long> roomIds) {
        return roomIds.stream()
                .collect(java.util.stream.Collectors.toMap(
                        roomId -> roomId,
                        this::getRoomOnlineUserCount
                ));
    }
}