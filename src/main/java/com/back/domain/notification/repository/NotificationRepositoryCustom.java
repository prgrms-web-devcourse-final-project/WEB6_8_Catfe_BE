package com.back.domain.notification.repository;

import com.back.domain.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationRepositoryCustom {
    Page<Notification> findByUserIdOrSystemType(Long userId, Pageable pageable);
    long countUnreadByUserId(Long userId);
    Page<Notification> findUnreadByUserId(Long userId, Pageable pageable);
}
