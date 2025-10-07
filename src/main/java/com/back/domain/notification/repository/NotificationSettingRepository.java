package com.back.domain.notification.repository;

import com.back.domain.notification.entity.NotificationSetting;
import com.back.domain.notification.entity.NotificationSettingType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {

    // 특정 사용자의 모든 알림 설정 조회
    @Query("SELECT ns FROM NotificationSetting ns WHERE ns.user.id = :userId")
    List<NotificationSetting> findAllByUserId(@Param("userId") Long userId);

    // 특정 사용자의 특정 타입 알림 설정 조회
    @Query("SELECT ns FROM NotificationSetting ns WHERE ns.user.id = :userId AND ns.type = :type")
    Optional<NotificationSetting> findByUserIdAndType(
            @Param("userId") Long userId,
            @Param("type") NotificationSettingType type
    );

    // 특정 사용자의 설정이 존재하는지 확인
    @Query("SELECT COUNT(ns) > 0 FROM NotificationSetting ns WHERE ns.user.id = :userId")
    boolean existsByUserId(@Param("userId") Long userId);

    // 특정 사용자의 활성화된 알림 설정만 조회
    @Query("SELECT ns FROM NotificationSetting ns WHERE ns.user.id = :userId AND ns.enabled = true")
    List<NotificationSetting> findEnabledSettingsByUserId(@Param("userId") Long userId);
}