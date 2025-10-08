package com.back.domain.notification.repository;

import com.back.domain.notification.entity.NotificationSetting;
import com.back.domain.notification.entity.NotificationSettingType;

import java.util.List;
import java.util.Optional;

public interface NotificationSettingRepositoryCustom {

    // 특정 사용자의 모든 알림 설정 조회
    List<NotificationSetting> findAllByUserId(Long userId);

    // 특정 사용자의 특정 타입 알림 설정 조회
    Optional<NotificationSetting> findByUserIdAndType(Long userId, NotificationSettingType type);

    // 특정 사용자의 설정이 존재하는지 확인
    boolean existsByUserId(Long userId);

    // 특정 사용자의 활성화된 알림 설정만 조회
    List<NotificationSetting> findEnabledSettingsByUserId(Long userId);
}