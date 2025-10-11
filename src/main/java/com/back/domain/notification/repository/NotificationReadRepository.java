package com.back.domain.notification.repository;

import com.back.domain.notification.entity.NotificationRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface NotificationReadRepository extends JpaRepository<NotificationRead, Long> {

    // 특정 유저가 특정 알림을 읽었는지 확인
    boolean existsByNotificationIdAndUserId(Long notificationId, Long userId);

    // 특정 유저의 특정 알림 읽음 기록 조회
    Optional<NotificationRead> findByNotificationIdAndUserId(Long notificationId, Long userId);

    // 특정 알림의 모든 읽음 기록 삭제
    void deleteByNotificationId(Long notificationId);

    // 특정 유저가 읽은 알림 ID 목록 조회
    @Query("SELECT nr.notification.id FROM NotificationRead nr WHERE nr.user.id = :userId AND nr.notification.id IN :notificationIds")
    Set<Long> findReadNotificationIdsByUserIdAndNotificationIdsIn(@Param("userId") Long userId, @Param("notificationIds") List<Long> notificationIds);
}
