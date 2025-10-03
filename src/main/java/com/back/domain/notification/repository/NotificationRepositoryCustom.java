package com.back.domain.notification.repository;

import com.back.domain.notification.entity.Notification;
import com.back.domain.notification.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NotificationRepositoryCustom {
    // 특정 유저의 알림 목록 조회 (개인 알림 + 시스템 알림)
    Page<Notification> findByUserIdOrSystemType(Long userId, Pageable pageable);

    // 특정 유저의 읽지 않은 알림 개수 조회
    long countUnreadByUserId(Long userId);

    // 특정 유저의 읽지 않은 알림 목록 조회
    Page<Notification> findUnreadByUserId(Long userId, Pageable pageable);
}
