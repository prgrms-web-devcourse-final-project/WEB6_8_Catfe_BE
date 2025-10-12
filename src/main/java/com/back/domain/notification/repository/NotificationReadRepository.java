package com.back.domain.notification.repository;

import com.back.domain.notification.entity.NotificationRead;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationReadRepository extends JpaRepository<NotificationRead, Long>, NotificationReadRepositoryCustom {

    // 특정 유저가 특정 알림을 읽었는지 확인
    boolean existsByNotificationIdAndUserId(Long notificationId, Long userId);

    // 특정 유저의 특정 알림 읽음 기록 조회
    Optional<NotificationRead> findByNotificationIdAndUserId(Long notificationId, Long userId);

    // 특정 알림의 모든 읽음 기록 삭제
    void deleteByNotificationId(Long notificationId);
}
