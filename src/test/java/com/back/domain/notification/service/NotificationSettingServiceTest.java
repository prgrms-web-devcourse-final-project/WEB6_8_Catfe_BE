package com.back.domain.notification.service;

import com.back.domain.notification.dto.NotificationSettingDto.*;
import com.back.domain.notification.entity.NotificationSetting;
import com.back.domain.notification.entity.NotificationSettingType;
import com.back.domain.notification.repository.NotificationSettingRepository;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import com.back.global.exception.CustomException;
import com.back.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationSettingService 테스트")
class NotificationSettingServiceTest {

    @Mock
    private NotificationSettingRepository settingRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationSettingService settingService;

    private User user;
    private List<NotificationSetting> defaultSettings;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .username("testuser")
                .email("test@test.com")
                .build();

        defaultSettings = createDefaultSettings(user);
    }

    private List<NotificationSetting> createDefaultSettings(User user) {
        return Arrays.stream(NotificationSettingType.values())
                .map(type -> NotificationSetting.create(user, type))
                .toList();
    }

    @Nested
    @DisplayName("알림 설정 조회 테스트")
    class GetSettingsTest {

        @Test
        @DisplayName("사용자의 알림 설정 조회 - 설정이 존재하는 경우")
        void t1() {
            // given
            given(settingRepository.findAllByUserId(user.getId()))
                    .willReturn(defaultSettings);

            // when
            SettingsResponse response = settingService.getUserSettings(user.getId());

            // then
            assertThat(response).isNotNull();
            assertThat(response.allEnabled()).isTrue();
            assertThat(response.settings()).hasSize(5);
            verify(settingRepository).findAllByUserId(user.getId());
        }

        @Test
        @DisplayName("사용자의 알림 설정 조회 - 설정이 없는 경우 자동 초기화")
        void t2() {
            // given
            given(settingRepository.findAllByUserId(user.getId()))
                    .willReturn(Collections.emptyList());
            given(userRepository.findById(user.getId()))
                    .willReturn(Optional.of(user));
            given(settingRepository.save(any(NotificationSetting.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            SettingsResponse response = settingService.getUserSettings(user.getId());

            // then
            assertThat(response).isNotNull();
            assertThat(response.allEnabled()).isTrue();
            assertThat(response.settings()).hasSize(5);
            verify(settingRepository).findAllByUserId(user.getId());
            verify(userRepository).findById(user.getId());
            verify(settingRepository, times(5)).save(any(NotificationSetting.class));
        }

        @Test
        @DisplayName("일부 설정이 비활성화된 경우 allEnabled는 false")
        void t3() {
            // given
            List<NotificationSetting> settings = createDefaultSettings(user);
            settings.get(0).disable(); // 첫 번째 설정 비활성화

            given(settingRepository.findAllByUserId(user.getId()))
                    .willReturn(settings);

            // when
            SettingsResponse response = settingService.getUserSettings(user.getId());

            // then
            assertThat(response).isNotNull();
            assertThat(response.allEnabled()).isFalse();
            assertThat(response.settings()).hasSize(5);
        }
    }

    @Nested
    @DisplayName("개별 알림 설정 토글 테스트")
    class ToggleSettingTest {

        @Test
        @DisplayName("개별 알림 설정 토글 - 활성화에서 비활성화")
        void t1() {
            // given
            NotificationSetting setting = defaultSettings.get(0);
            assertThat(setting.isEnabled()).isTrue();

            given(settingRepository.findByUserIdAndType(user.getId(), NotificationSettingType.SYSTEM))
                    .willReturn(Optional.of(setting));

            // when
            settingService.toggleSetting(user.getId(), NotificationSettingType.SYSTEM);

            // then
            assertThat(setting.isEnabled()).isFalse();
            verify(settingRepository).findByUserIdAndType(user.getId(), NotificationSettingType.SYSTEM);
        }

        @Test
        @DisplayName("개별 알림 설정 토글 - 비활성화에서 활성화")
        void t2() {
            // given
            NotificationSetting setting = defaultSettings.get(0);
            setting.disable();
            assertThat(setting.isEnabled()).isFalse();

            given(settingRepository.findByUserIdAndType(user.getId(), NotificationSettingType.SYSTEM))
                    .willReturn(Optional.of(setting));

            // when
            settingService.toggleSetting(user.getId(), NotificationSettingType.SYSTEM);

            // then
            assertThat(setting.isEnabled()).isTrue();
            verify(settingRepository).findByUserIdAndType(user.getId(), NotificationSettingType.SYSTEM);
        }

        @Test
        @DisplayName("개별 알림 설정 토글 - 설정이 없는 경우 예외 발생")
        void t3() {
            // given
            given(settingRepository.findByUserIdAndType(user.getId(), NotificationSettingType.SYSTEM))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    settingService.toggleSetting(user.getId(), NotificationSettingType.SYSTEM)
            ).isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTIFICATION_SETTING_NOT_FOUND);

            verify(settingRepository).findByUserIdAndType(user.getId(), NotificationSettingType.SYSTEM);
        }
    }

    @Nested
    @DisplayName("전체 알림 ON/OFF 테스트")
    class ToggleAllSettingsTest {

        @Test
        @DisplayName("전체 알림 비활성화")
        void t1() {
            // given
            given(settingRepository.findAllByUserId(user.getId()))
                    .willReturn(defaultSettings);

            // when
            settingService.toggleAllSettings(user.getId(), false);

            // then
            assertThat(defaultSettings).allMatch(setting -> !setting.isEnabled());
            verify(settingRepository).findAllByUserId(user.getId());
        }

        @Test
        @DisplayName("전체 알림 활성화")
        void t2() {
            // given
            defaultSettings.forEach(NotificationSetting::disable);
            given(settingRepository.findAllByUserId(user.getId()))
                    .willReturn(defaultSettings);

            // when
            settingService.toggleAllSettings(user.getId(), true);

            // then
            assertThat(defaultSettings).allMatch(NotificationSetting::isEnabled);
            verify(settingRepository).findAllByUserId(user.getId());
        }

        @Test
        @DisplayName("전체 알림 ON/OFF - 설정이 없는 경우 예외 발생")
        void t3() {
            // given
            given(settingRepository.findAllByUserId(user.getId()))
                    .willReturn(Collections.emptyList());

            // when & then
            assertThatThrownBy(() ->
                    settingService.toggleAllSettings(user.getId(), true)
            ).isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTIFICATION_SETTING_NOT_FOUND);

            verify(settingRepository).findAllByUserId(user.getId());
        }
    }

    @Nested
    @DisplayName("기본 알림 설정 초기화 테스트")
    class InitializeDefaultSettingsTest {

        @Test
        @DisplayName("기본 알림 설정 초기화 - 5개 설정 생성")
        void t1() {
            // given
            given(userRepository.findById(user.getId()))
                    .willReturn(Optional.of(user));
            given(settingRepository.save(any(NotificationSetting.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            List<NotificationSetting> settings = settingService.initializeDefaultSettings(user.getId());

            // then
            assertThat(settings).hasSize(5);
            assertThat(settings).allMatch(NotificationSetting::isEnabled);
            verify(userRepository).findById(user.getId());
            verify(settingRepository, times(5)).save(any(NotificationSetting.class));
        }

        @Test
        @DisplayName("기본 알림 설정 초기화 - 사용자가 없는 경우 예외 발생")
        void t2() {
            // given
            given(userRepository.findById(999L))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    settingService.initializeDefaultSettings(999L)
            ).isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

            verify(userRepository).findById(999L);
            verify(settingRepository, never()).save(any(NotificationSetting.class));
        }

        @Test
        @DisplayName("기본 알림 설정 초기화 - 모든 타입이 포함되는지 확인")
        void t3() {
            // given
            given(userRepository.findById(user.getId()))
                    .willReturn(Optional.of(user));
            given(settingRepository.save(any(NotificationSetting.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            List<NotificationSetting> settings = settingService.initializeDefaultSettings(user.getId());

            // then
            Set<NotificationSettingType> types = settings.stream()
                    .map(NotificationSetting::getType)
                    .collect(java.util.stream.Collectors.toSet());

            assertThat(types).containsExactlyInAnyOrder(NotificationSettingType.values());
        }
    }

    @Nested
    @DisplayName("알림 활성화 여부 확인 테스트")
    class IsNotificationEnabledTest {

        @Test
        @DisplayName("알림이 활성화된 경우 true 반환")
        void t1() {
            // given
            NotificationSetting setting = defaultSettings.get(0);
            given(settingRepository.findByUserIdAndType(user.getId(), NotificationSettingType.SYSTEM))
                    .willReturn(Optional.of(setting));

            // when
            boolean result = settingService.isNotificationEnabled(
                    user.getId(),
                    NotificationSettingType.SYSTEM
            );

            // then
            assertThat(result).isTrue();
            verify(settingRepository).findByUserIdAndType(user.getId(), NotificationSettingType.SYSTEM);
        }

        @Test
        @DisplayName("알림이 비활성화된 경우 false 반환")
        void t2() {
            // given
            NotificationSetting setting = defaultSettings.get(0);
            setting.disable();
            given(settingRepository.findByUserIdAndType(user.getId(), NotificationSettingType.SYSTEM))
                    .willReturn(Optional.of(setting));

            // when
            boolean result = settingService.isNotificationEnabled(
                    user.getId(),
                    NotificationSettingType.SYSTEM
            );

            // then
            assertThat(result).isFalse();
            verify(settingRepository).findByUserIdAndType(user.getId(), NotificationSettingType.SYSTEM);
        }

        @Test
        @DisplayName("설정이 없는 경우 기본값 true 반환")
        void t3() {
            // given
            given(settingRepository.findByUserIdAndType(user.getId(), NotificationSettingType.SYSTEM))
                    .willReturn(Optional.empty());

            // when
            boolean result = settingService.isNotificationEnabled(
                    user.getId(),
                    NotificationSettingType.SYSTEM
            );

            // then
            assertThat(result).isTrue();
            verify(settingRepository).findByUserIdAndType(user.getId(), NotificationSettingType.SYSTEM);
        }
    }
}