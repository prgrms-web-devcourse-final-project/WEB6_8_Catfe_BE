package com.back.domain.notification.service;

import com.back.domain.notification.dto.NotificationSettingDto.*;
import com.back.domain.notification.entity.NotificationSetting;
import com.back.domain.notification.entity.NotificationSettingType;
import com.back.domain.notification.repository.NotificationSettingRepository;
import com.back.domain.user.common.entity.User;
import com.back.domain.user.common.repository.UserRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSettingService {

    private final NotificationSettingRepository settingRepository;
    private final UserRepository userRepository;

    // 사용자의 모든 알림 설정 조회
    @Transactional(readOnly = true)
    public SettingsResponse getUserSettings(Long userId) {
        log.info("사용자 알림 설정 조회 - userId: {}", userId);

        List<NotificationSetting> settings = settingRepository.findAllByUserId(userId);

        // 설정이 없으면 기본값으로 생성
        if (settings.isEmpty()) {
            log.info("알림 설정이 없어 기본값으로 초기화 - userId: {}", userId);
            settings = initializeDefaultSettings(userId);
        }

        // 전체 알림이 모두 활성화되어 있는지 확인
        boolean allEnabled = settings.stream()
                .allMatch(NotificationSetting::isEnabled);

        return SettingsResponse.of(allEnabled, settings);
    }

    // 개별 알림 설정 토글
    @Transactional
    public void toggleSetting(Long userId, NotificationSettingType type) {
        log.info("알림 설정 토글 - userId: {}, type: {}", userId, type);

        NotificationSetting setting = settingRepository
                .findByUserIdAndType(userId, type)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_SETTING_NOT_FOUND));

        setting.toggle();
        log.info("알림 설정 토글 완료 - type: {}, enabled: {}", type, setting.isEnabled());
    }

    // 전체 알림 ON/OFF
    @Transactional
    public void toggleAllSettings(Long userId, boolean enable) {
        log.info("전체 알림 설정 변경 - userId: {}, enable: {}", userId, enable);

        List<NotificationSetting> settings = settingRepository.findAllByUserId(userId);

        if (settings.isEmpty()) {
            throw new CustomException(ErrorCode.NOTIFICATION_SETTING_NOT_FOUND);
        }

        settings.forEach(setting -> {
            if (enable) {
                setting.enable();
            } else {
                setting.disable();
            }
        });

        log.info("전체 알림 설정 변경 완료 - 변경된 설정 개수: {}", settings.size());
    }

    // 기본 알림 설정 초기화
    @Transactional
    public List<NotificationSetting> initializeDefaultSettings(Long userId) {
        log.info("기본 알림 설정 초기화 - userId: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 모든 알림 타입에 대해 기본 활성화 상태로 생성
        List<NotificationSetting> settings = Arrays.stream(NotificationSettingType.values())
                .map(type -> NotificationSetting.create(user, type))
                .map(settingRepository::save)
                .toList();

        log.info("기본 알림 설정 초기화 완료 - 생성된 설정 개수: {}", settings.size());

        return settings;
    }

    // 특정 알림 타입이 활성화되어 있는지 확인
    @Transactional(readOnly = true)
    public boolean isNotificationEnabled(Long userId, NotificationSettingType type) {
        return settingRepository.findByUserIdAndType(userId, type)
                .map(NotificationSetting::isEnabled)
                .orElse(true); // 설정이 없으면 기본값 true (활성화)
    }
}