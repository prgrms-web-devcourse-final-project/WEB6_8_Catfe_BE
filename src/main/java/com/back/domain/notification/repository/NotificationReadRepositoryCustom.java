package com.back.domain.notification.repository;

import java.util.List;
import java.util.Set;

public interface NotificationReadRepositoryCustom {

    // 특정 유저가 읽은 알림 ID 목록을 한 번에 조회
    Set<Long> findReadNotificationIds(Long userId, List<Long> notificationIds);
}
