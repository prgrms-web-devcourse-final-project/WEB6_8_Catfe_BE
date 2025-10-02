package com.back.domain.notification.repository;

import com.back.domain.notification.entity.Notification;
import com.back.domain.notification.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long>, NotificationRepositoryCustom {
    Page<Notification> findByRoomIdOrderByCreatedAtDesc(Long roomId, Pageable pageable);
    List<Notification> findByType(NotificationType type);
}