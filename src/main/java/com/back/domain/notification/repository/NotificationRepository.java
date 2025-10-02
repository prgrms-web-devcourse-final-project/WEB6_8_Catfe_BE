package com.back.domain.notification.repository;

import com.back.domain.notification.entity.Notification;
import com.back.domain.notification.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 특정 유저의 알림 목록 조회 (개인 알림 + 시스템 알림)
    @Query("SELECT n FROM Notification n " +
            "WHERE n.user.id = :userId OR n.type = 'SYSTEM' " +
            "ORDER BY n.createdAt DESC")
    Page<Notification> findByUserIdOrSystemType(@Param("userId") Long userId, Pageable pageable);

    // 특정 유저의 읽지 않은 알림 개수 조회
    @Query("SELECT COUNT(n) FROM Notification n " +
            "LEFT JOIN NotificationRead nr ON n.id = nr.notification.id AND nr.user.id = :userId " +
            "WHERE (n.user.id = :userId OR n.type = 'SYSTEM') " +
            "AND nr.id IS NULL")
    long countUnreadByUserId(@Param("userId") Long userId);

    // 특정 유저의 읽지 않은 알림 목록 조회
    @Query("SELECT n FROM Notification n " +
            "LEFT JOIN NotificationRead nr ON n.id = nr.notification.id AND nr.user.id = :userId " +
            "WHERE (n.user.id = :userId OR n.type = 'SYSTEM') " +
            "AND nr.id IS NULL " +
            "ORDER BY n.createdAt DESC")
    Page<Notification> findUnreadByUserId(@Param("userId") Long userId, Pageable pageable);

    // 특정 스터디룸의 알림 조회
    Page<Notification> findByRoomIdOrderByCreatedAtDesc(Long roomId, Pageable pageable);

    // 특정 타입의 알림 조회
    List<Notification> findByType(NotificationType type);
}