package com.back.domain.notification.service;

import com.back.domain.notification.dto.NotificationWebSocketDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationWebSocketService { // WebSocket을 통한 실시간 알림 전송 서비스

    private final SimpMessagingTemplate messagingTemplate;

    // 특정 유저에게 알림 전송
    public void sendNotificationToUser(Long userId, NotificationWebSocketDto notificationDto) {
        try {
            String destination = "/topic/user/" + userId + "/notifications";
            messagingTemplate.convertAndSend(destination, notificationDto);

            log.info("실시간 알림 전송 성공 - 유저 ID: {}, 알림 ID: {}, 제목: {}",
                    userId, notificationDto.notificationId(), notificationDto.title());

        } catch (Exception e) {
            log.error("실시간 알림 전송 실패 - 유저 ID: {}, 오류: {}", userId, e.getMessage(), e);
        }
    }

    // 전체 유저에게 시스템 알림 브로드캐스트
    public void broadcastSystemNotification(NotificationWebSocketDto notificationDto) {
        try {
            String destination = "/topic/notifications/system";
            messagingTemplate.convertAndSend(destination, notificationDto);

            log.info("시스템 알림 브로드캐스트 성공 - 알림 ID: {}, 제목: {}",
                    notificationDto.notificationId(), notificationDto.title());

        } catch (Exception e) {
            log.error("시스템 알림 브로드캐스트 실패 - 오류: {}", e.getMessage(), e);
        }
    }

    // 스터디룸 멤버들에게 알림 전송
    public void sendNotificationToRoom(Long roomId, NotificationWebSocketDto notificationDto) {
        try {
            String destination = "/topic/room/" + roomId + "/notifications";
            messagingTemplate.convertAndSend(destination, notificationDto);

            log.info("스터디룸 알림 전송 성공 - 룸 ID: {}, 알림 ID: {}, 제목: {}",
                    roomId, notificationDto.notificationId(), notificationDto.title());

        } catch (Exception e) {
            log.error("스터디룸 알림 전송 실패 - 룸 ID: {}, 오류: {}", roomId, e.getMessage(), e);
        }
    }
}