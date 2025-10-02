package com.back.domain.notification.service;

import com.back.domain.notification.dto.NotificationWebSocketDto;
import com.back.domain.notification.entity.Notification;
import com.back.domain.notification.entity.NotificationRead;
import com.back.domain.notification.repository.NotificationReadRepository;
import com.back.domain.notification.repository.NotificationRepository;
import com.back.domain.studyroom.entity.Room;
import com.back.domain.user.entity.User;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationReadRepository notificationReadRepository;
    private final NotificationWebSocketService webSocketService;

    // ==================== 알림 생성 및 전송 ====================

    // 개인 알림 생성 및 전송
    @Transactional
    public Notification createPersonalNotification(User user, String title, String content, String targetUrl) {

        // DB에 알림 저장
        Notification notification = Notification.createPersonalNotification(user, title, content, targetUrl);
        notificationRepository.save(notification);

        // WebSocket으로 실시간 전송
        NotificationWebSocketDto dto = NotificationWebSocketDto.from(
                notification.getId(),
                notification.getTitle(),
                notification.getContent(),
                notification.getType(),
                notification.getTargetUrl(),
                notification.getCreatedAt()
        );
        webSocketService.sendNotificationToUser(user.getId(), dto);

        log.info("개인 알림 생성 - 유저 ID: {}, 알림 ID: {}", user.getId(), notification.getId());
        return notification;
    }

    // 스터디룸 알림 생성 및 전송
    @Transactional
    public Notification createRoomNotification(Room room, String title, String content, String targetUrl) {

        Notification notification = Notification.createRoomNotification(room, title, content, targetUrl);
        notificationRepository.save(notification);

        NotificationWebSocketDto dto = NotificationWebSocketDto.from(
                notification.getId(),
                notification.getTitle(),
                notification.getContent(),
                notification.getType(),
                notification.getTargetUrl(),
                notification.getCreatedAt()
        );
        webSocketService.sendNotificationToRoom(room.getId(), dto);

        log.info("스터디룸 알림 생성 - 룸 ID: {}, 알림 ID: {}", room.getId(), notification.getId());
        return notification;
    }

    // 시스템 전체 알림 생성 및 브로드캐스트
    @Transactional
    public Notification createSystemNotification(String title, String content, String targetUrl) {

        Notification notification = Notification.createSystemNotification(title, content, targetUrl);
        notificationRepository.save(notification);

        NotificationWebSocketDto dto = NotificationWebSocketDto.from(
                notification.getId(),
                notification.getTitle(),
                notification.getContent(),
                notification.getType(),
                notification.getTargetUrl(),
                notification.getCreatedAt()
        );
        webSocketService.broadcastSystemNotification(dto);

        log.info("시스템 알림 생성 - 알림 ID: {}", notification.getId());
        return notification;
    }

    // 커뮤니티 알림 생성 및 전송
    @Transactional
    public Notification createCommunityNotification(User user, String title, String content, String targetUrl) {

        Notification notification = Notification.createCommunityNotification(user, title, content, targetUrl);
        notificationRepository.save(notification);

        NotificationWebSocketDto dto = NotificationWebSocketDto.from(
                notification.getId(),
                notification.getTitle(),
                notification.getContent(),
                notification.getType(),
                notification.getTargetUrl(),
                notification.getCreatedAt()
        );
        webSocketService.sendNotificationToUser(user.getId(), dto);

        log.info("커뮤니티 알림 생성 - 유저 ID: {}, 알림 ID: {}", user.getId(), notification.getId());
        return notification;
    }

    // ==================== 알림 조회 ====================

    // 특정 유저의 알림 목록 조회 (개인 알림 + 시스템 알림)
    public Page<Notification> getUserNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrSystemType(userId, pageable);
    }

    // 특정 유저의 읽지 않은 알림 목록 조회
    public Page<Notification> getUnreadNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findUnreadByUserId(userId, pageable);
    }

    // 특정 유저의 읽지 않은 알림 개수 조회
    public long getUnreadCount(Long userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    // 알림 단건 조회
    public Notification getNotification(Long notificationId) {
        return notificationRepository.findById(notificationId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND));
    }

    // 특정 유저가 특정 알림을 읽었는지 확인
    public boolean isNotificationRead(Long notificationId, Long userId) {
        return notificationReadRepository.existsByNotificationIdAndUserId(notificationId, userId);
    }

    // ==================== 알림 읽음 처리 ====================

    // 알림 읽음 처리
    @Transactional
    public void markAsRead(Long notificationId, User user) {
        // 1. 알림 존재 확인
        Notification notification = getNotification(notificationId);

        // 2. 이미 읽은 알림인지 확인
        if (notificationReadRepository.existsByNotificationIdAndUserId(notificationId, user.getId())) {
            log.debug("이미 읽은 알림 - 알림 ID: {}, 유저 ID: {}", notificationId, user.getId());
            return;
        }

        // 3. 읽음 기록 생성
        NotificationRead notificationRead = NotificationRead.create(notification, user);
        notificationReadRepository.save(notificationRead);

        // 4. 알림 상태 업데이트 (선택적)
        notification.markAsRead();

        log.info("알림 읽음 처리 - 알림 ID: {}, 유저 ID: {}", notificationId, user.getId());
    }

    // 여러 알림 일괄 읽음 처리
    @Transactional
    public void markMultipleAsRead(Long userId, User user) {
        Page<Notification> unreadNotifications = getUnreadNotifications(userId, Pageable.unpaged());

        for (Notification notification : unreadNotifications) {
            if (!notificationReadRepository.existsByNotificationIdAndUserId(notification.getId(), user.getId())) {
                NotificationRead notificationRead = NotificationRead.create(notification, user);
                notificationReadRepository.save(notificationRead);
                notification.markAsRead();
            }
        }

        log.info("일괄 읽음 처리 - 유저 ID: {}, 처리 개수: {}", userId, unreadNotifications.getTotalElements());
    }
}