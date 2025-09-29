package com.back.global.websocket.service;

import com.back.domain.studyroom.dto.RoomBroadcastMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * WebSocket 브로드캐스트 전용 서비스
 * - 메시지 전송에만 집중
 * - SimpMessagingTemplate 의존성을 여기서만 관리
 * - 세션 관리와 분리하여 순환 참조 방지
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketSessionManager sessionManager;

    /**
     * 특정 방의 모든 온라인 사용자에게 메시지 브로드캐스트
     */
    public void broadcastToRoom(Long roomId, RoomBroadcastMessage message) {
        try {
            Set<Long> onlineUsers = sessionManager.getOnlineUsersInRoom(roomId);

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
            // 예외를 던지지 않고 로깅만 (브로드캐스트 실패가 핵심 기능을 막지 않도록)
        }
    }

    /**
     * 특정 사용자에게 개인 메시지 전송
     */
    public void sendToUser(Long userId, String destination, Object message) {
        try {
            if (sessionManager.isUserConnected(userId)) {
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
            Set<Long> onlineUsers = sessionManager.getOnlineUsersInRoom(roomId);

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
