package com.back.global.websocket.service;

import com.back.domain.user.common.entity.User;
import com.back.domain.user.common.repository.UserRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import com.back.global.websocket.dto.WebSocketSessionInfo;
import com.back.global.websocket.event.SessionDisconnectedEvent;
import com.back.global.websocket.event.UserJoinedEvent;
import com.back.global.websocket.event.UserLeftEvent;
import com.back.global.websocket.store.RedisSessionStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
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
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    // 세션 종료 이벤트 리스너
    @EventListener
    @Transactional
    public void handleSessionDisconnected(SessionDisconnectedEvent event) {
        Long userId = event.getUserId();
        log.info("[이벤트 수신] 세션 종료 이벤트 수신하여 퇴장 처리 시작 - 사용자: {}", userId);
        exitAllRooms(userId);
    }

    // 사용자 방 입장 (아바타 정보 포함)
    public void enterRoom(Long userId, Long roomId, Long avatarId) {
        WebSocketSessionInfo sessionInfo = redisSessionStore.getUserSession(userId);

        if (sessionInfo == null) {
            log.warn("세션 정보가 없어 방 입장 실패 - 사용자: {}, 방: {}", userId, roomId);
            throw new CustomException(ErrorCode.WS_SESSION_NOT_FOUND);
        }

        if (sessionInfo.currentRoomId() != null && !sessionInfo.currentRoomId().equals(roomId)) {
            exitRoom(userId, sessionInfo.currentRoomId());
            log.debug("기존 방에서 퇴장 처리 완료 - 사용자: {}, 이전 방: {}",
                    userId, sessionInfo.currentRoomId());
        }

        WebSocketSessionInfo updatedSession = sessionInfo.withRoomId(roomId);
        redisSessionStore.saveUserSession(userId, updatedSession);
        redisSessionStore.addUserToRoom(roomId, userId);
        
        // 아바타 정보 저장
        saveUserAvatar(roomId, userId, avatarId);

        log.info("방 입장 완료 - 사용자: {}, 방: {}, 아바타: {}", userId, roomId, avatarId);

        broadcastUserJoined(roomId, userId, avatarId);
    }
    
    // 기존 메서드 호환성 유지 (아바타 없이 입장)
    public void enterRoom(Long userId, Long roomId) {
        enterRoom(userId, roomId, null);
    }

    // 사용자 방 퇴장
    public void exitRoom(Long userId, Long roomId) {
        WebSocketSessionInfo sessionInfo = redisSessionStore.getUserSession(userId);
        if (sessionInfo != null) {
            WebSocketSessionInfo updatedSession = sessionInfo.withoutRoom();
            redisSessionStore.saveUserSession(userId, updatedSession);
        }
        redisSessionStore.removeUserFromRoom(roomId, userId);
        log.info("방 퇴장 완료 - 사용자: {}, 방: {}", userId, roomId);
        broadcastUserLeft(roomId, userId);
    }

    public void exitAllRooms(Long userId) {
        try {
            Long currentRoomId = getCurrentRoomId(userId);
            if (currentRoomId != null) {
                exitRoom(userId, currentRoomId);
                log.info("모든 방에서 퇴장 처리 완료 - 사용자: {}", userId);
            }
        } catch (Exception e) {
            log.error("모든 방 퇴장 처리 실패 - 사용자: {}", userId, e);
        }
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

    /**
     * 여러 방의 온라인 참가자 수를 일괄 조회
     * N+1 문제 해결을 위한 일괄 조회 메서드
     * @param roomIds 방 ID 목록
     * @return 방 ID → 참가자 수 맵
     */
    public Map<Long, Long> getParticipantCounts(java.util.List<Long> roomIds) {
        return redisSessionStore.getRoomUserCounts(roomIds);
    }
    
    // ==================== 아바타 관련 메서드 ====================
    
    /**
     * 사용자의 아바타 정보 저장 (Redis)
     * @param roomId 방 ID
     * @param userId 사용자 ID
     * @param avatarId 아바타 ID
     */
    private void saveUserAvatar(Long roomId, Long userId, Long avatarId) {
        if (avatarId == null) return;
        String avatarKey = buildAvatarKey(roomId, userId);
        redisSessionStore.saveValue(avatarKey, avatarId.toString(), java.time.Duration.ofMinutes(6));
        log.debug("아바타 정보 저장 - RoomId: {}, UserId: {}, AvatarId: {}", roomId, userId, avatarId);
    }

    /**
     * 사용자의 아바타 ID 조회
     * @param roomId 방 ID
     * @param userId 사용자 ID
     * @return 아바타 ID (없으면 null)
     */
    public Long getUserAvatar(Long roomId, Long userId) {
        String avatarKey = buildAvatarKey(roomId, userId);
        String avatarIdStr = redisSessionStore.getValue(avatarKey);
        
        if (avatarIdStr == null) {
            return null;
        }
        
        try {
            return Long.parseLong(avatarIdStr);
        } catch (NumberFormatException e) {
            log.warn("아바타 ID 파싱 실패 - RoomId: {}, UserId: {}, Value: {}", roomId, userId, avatarIdStr);
            return null;
        }
    }

    /**
     * 여러 사용자의 아바타 ID 일괄 조회 (N+1 방지)
     * @param roomId 방 ID
     * @param userIds 사용자 ID 목록
     * @return 사용자 ID → 아바타 ID 맵
     */
    public Map<Long, Long> getUserAvatars(Long roomId, Set<Long> userIds) {
        Map<Long, Long> result = new java.util.HashMap<>();
        for (Long userId : userIds) {
            Long avatarId = getUserAvatar(roomId, userId);
            if (avatarId != null) {
                result.put(userId, avatarId);
            }
        }
        
        return result;
    }
    
    /**
     * 아바타 Redis Key 생성
     */
    private String buildAvatarKey(Long roomId, Long userId) {
        return "ws:room:" + roomId + ":user:" + userId + ":avatar";
    }
    
    /**
     * 아바타 정보 업데이트 (외부에서 호출 가능)
     * VISITOR가 아바타를 변경할 때 사용
     * @param roomId 방 ID
     * @param userId 사용자 ID
     * @param avatarId 새 아바타 ID
     */
    public void updateUserAvatar(Long roomId, Long userId, Long avatarId) {
        if (avatarId == null) return;
        String avatarKey = buildAvatarKey(roomId, userId);
        redisSessionStore.saveValue(avatarKey, avatarId.toString(), java.time.Duration.ofMinutes(6));
        log.info("아바타 업데이트 (Redis) - RoomId: {}, UserId: {}, AvatarId: {}", roomId, userId, avatarId);
    }

    private void broadcastUserJoined(Long roomId, Long userId, Long avatarId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.error("📢 [방송 실패] 사용자 정보를 찾을 수 없어 입장 알림을 보낼 수 없습니다. userId: {}", userId);
            return;
        }
        UserJoinedEvent event = new UserJoinedEvent(user.getId(), user.getNickname(), user.getProfileImageUrl(), avatarId);
        String destination = "/topic/room/" + roomId + "/events";
        messagingTemplate.convertAndSend(destination, event);
        log.info("📢 [방송] 사용자 입장 알림 - 방: {}, 사용자: {}", roomId, userId);
    }

    private void broadcastUserLeft(Long roomId, Long userId) {
        UserLeftEvent event = new UserLeftEvent(userId);
        String destination = "/topic/room/" + roomId + "/events";
        messagingTemplate.convertAndSend(destination, event);
        log.info("📢 [방송] 사용자 퇴장 알림 - 방: {}, 사용자: {}", roomId, userId);
    }
}
