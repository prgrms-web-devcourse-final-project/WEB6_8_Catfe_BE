package com.back.domain.notification.repository;

import com.back.domain.notification.entity.NotificationSetting;
import com.back.domain.notification.entity.NotificationSettingType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {

    // 특정 유저의 모든 알림 설정 조회
    List<NotificationSetting> findByUserId(Long userId);

    // 특정 유저의 특정 타입 알림 설정 조회
    Optional<NotificationSetting> findByUserIdAndType(Long userId, NotificationSettingType type);

    // 특정 유저의 특정 타입 알림 설정 존재 여부
    boolean existsByUserIdAndType(Long userId, NotificationSettingType type);

    // 특정 유저의 활성화된 알림 설정만 조회
    List<NotificationSetting> findByUserIdAndEnabledTrue(Long userId);
}