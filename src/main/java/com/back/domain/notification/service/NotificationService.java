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

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationReadRepository notificationReadRepository;
    private final NotificationWebSocketService webSocketService;

    // ==================== 검증 메서드 ====================

    // 발신자와 수신자 검증
    private void validateActorAndReceiver(User receiver, User actor) {
        if (receiver.getId().equals(actor.getId())) {
            log.debug("자기 자신에게 알림 전송 시도 - receiver: {}, actor: {}",
                    receiver.getId(), actor.getId());
            throw new CustomException(ErrorCode.NOTIFICATION_FORBIDDEN);
        }
    }

    // ==================== 알림 생성 및 전송 ====================

    // 개인 알림 생성 및 전송
    @Transactional
    public Notification createPersonalNotification(
            User receiver,
            User actor,
            String title,
            String content,
            String targetUrl) {

        // 자기 자신에게 알림 방지
        validateActorAndReceiver(receiver, actor);

        // DB에 알림 저장
        Notification notification = Notification.createPersonalNotification(
                receiver, actor, title, content, targetUrl);
        notificationRepository.save(notification);

        // WebSocket으로 실시간 전송
        NotificationWebSocketDto dto = NotificationWebSocketDto.from(notification);
        webSocketService.sendNotificationToUser(receiver.getId(), dto);

        log.info("개인 알림 생성 - 수신자 ID: {}, 발신자 ID: {}, 알림 ID: {}",
                receiver.getId(), actor.getId(), notification.getId());
        return notification;
    }

    // 스터디룸 알림 생성 및 전송
    @Transactional
    public Notification createRoomNotification(
            Room room,
            User actor,
            String title,
            String content,
            String targetUrl) {

        Notification notification = Notification.createRoomNotification(
                room, actor, title, content, targetUrl);
        notificationRepository.save(notification);

        NotificationWebSocketDto dto = NotificationWebSocketDto.from(notification);
        webSocketService.sendNotificationToRoom(room.getId(), dto);

        log.info("스터디룸 알림 생성 - 룸 ID: {}, 발신자 ID: {}, 알림 ID: {}",
                room.getId(), actor.getId(), notification.getId());
        return notification;
    }

    // 시스템 전체 알림 생성 및 브로드캐스트
    @Transactional
    public Notification createSystemNotification(String title, String content, String targetUrl) {

        Notification notification = Notification.createSystemNotification(title, content, targetUrl);
        notificationRepository.save(notification);

        NotificationWebSocketDto dto = NotificationWebSocketDto.from(notification);
        webSocketService.broadcastSystemNotification(dto);

        log.info("시스템 알림 생성 - 알림 ID: {}", notification.getId());
        return notification;
    }

    // 커뮤니티 알림 생성 및 전송
    @Transactional
    public Notification createCommunityNotification(
            User receiver,
            User actor,
            String title,
            String content,
            String targetUrl) {

        // 검증: 자기 자신에게 알림 방지
        validateActorAndReceiver(receiver, actor);

        Notification notification = Notification.createCommunityNotification(
                receiver, actor, title, content, targetUrl);
        notificationRepository.save(notification);

        NotificationWebSocketDto dto = NotificationWebSocketDto.from(notification);
        webSocketService.sendNotificationToUser(receiver.getId(), dto);

        log.info("커뮤니티 알림 생성 - 수신자 ID: {}, 발신자 ID: {}, 알림 ID: {}",
                receiver.getId(), actor.getId(), notification.getId());
        return notification;
    }

    // ==================== 알림 조회 ====================

    public Page<Notification> getUserNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrSystemType(userId, pageable);
    }

    public Page<Notification> getUnreadNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findUnreadByUserId(userId, pageable);
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    public Notification getNotification(Long notificationId) {
        return notificationRepository.findById(notificationId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND));
    }

    public boolean isNotificationRead(Long notificationId, Long userId) {
        return notificationReadRepository.existsByNotificationIdAndUserId(notificationId, userId);
    }

    // ==================== 알림 읽음 처리 ====================

    // 알림 읽음 처리
    @Transactional
    public void markAsRead(Long notificationId, User user) {
        // 알림 존재 확인
        Notification notification = getNotification(notificationId);

        // 알림 접근 권한 확인
        if (!notification.isVisibleToUser(user.getId())) {
            throw new CustomException(ErrorCode.NOTIFICATION_FORBIDDEN);
        }

        // 이미 읽은 알림인지 확인
        if (notificationReadRepository.existsByNotificationIdAndUserId(notificationId, user.getId())) {
            log.debug("이미 읽은 알림 - 알림 ID: {}, 유저 ID: {}", notificationId, user.getId());
            throw new CustomException(ErrorCode.NOTIFICATION_ALREADY_READ);
        }

        // 읽음 기록 생성
        NotificationRead notificationRead = NotificationRead.create(notification, user);
        notificationReadRepository.save(notificationRead);

        log.info("알림 읽음 처리 - 알림 ID: {}, 유저 ID: {}", notificationId, user.getId());
    }

    // 여러 알림 일괄 읽음 처리
    @Transactional
    public void markMultipleAsRead(Long userId, User user) {
        // Page가 아닌 List로 직접 조회
        List<Notification> unreadNotifications = notificationRepository.findAllUnreadByUserId(userId);

        List<NotificationRead> notificationReads = unreadNotifications.stream()
                .filter(notification -> !notificationReadRepository
                        .existsByNotificationIdAndUserId(notification.getId(), user.getId()))
                .map(notification -> NotificationRead.create(notification, user))
                .toList();

        if (!notificationReads.isEmpty()) {
            notificationReadRepository.saveAll(notificationReads);
            log.info("일괄 읽음 처리 - 유저 ID: {}, 처리 개수: {}", userId, notificationReads.size());
        } else {
            log.info("일괄 읽음 처리 - 유저 ID: {}, 읽을 알림 없음", userId);
        }
    }
}