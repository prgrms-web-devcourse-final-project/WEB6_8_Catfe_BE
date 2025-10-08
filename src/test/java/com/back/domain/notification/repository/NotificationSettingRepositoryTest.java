package com.back.domain.notification.repository;

import com.back.domain.notification.entity.NotificationSetting;
import com.back.domain.notification.entity.NotificationSettingType;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import com.back.global.config.QueryDslTestConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@Import(QueryDslTestConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class NotificationSettingRepositoryTest {

    @Autowired
    private NotificationSettingRepository notificationSettingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User userWithSettings;
    private User userWithoutSettings;

    @BeforeEach
    void setUp() {
        userWithSettings = User.builder().email("user1@test.com").username("유저1").build();
        userWithoutSettings = User.builder().email("user2@test.com").username("유저2").build();
        userRepository.saveAll(List.of(userWithSettings, userWithoutSettings));

        NotificationSetting commentSetting = NotificationSetting.create(
                userWithSettings,
                NotificationSettingType.POST_COMMENT
        );

        NotificationSetting likeSetting = NotificationSetting.create(
                userWithSettings,
                NotificationSettingType.POST_LIKE,
                false
        );

        notificationSettingRepository.saveAll(List.of(commentSetting, likeSetting));

        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("알림 설정 조회 테스트")
    class FindNotificationSettingsTest {

        @Test
        @DisplayName("특정 사용자의 모든 알림 설정 조회 성공")
        void t1() {
            // when
            List<NotificationSetting> settings = notificationSettingRepository.findAllByUserId(userWithSettings.getId());

            // then: Enum 타입이 정확한지 확인
            assertThat(settings).hasSize(2);
            assertThat(settings)
                    .extracting(NotificationSetting::getType)
                    .containsExactlyInAnyOrder(NotificationSettingType.POST_COMMENT, NotificationSettingType.POST_LIKE);
        }

        @Test
        @DisplayName("특정 사용자의 특정 타입 알림 설정 조회 성공")
        void t2() {
            // when
            Optional<NotificationSetting> settingOpt = notificationSettingRepository.findByUserIdAndType(
                    userWithSettings.getId(),
                    NotificationSettingType.POST_COMMENT
            );

            // then
            assertThat(settingOpt).isPresent();
            assertThat(settingOpt.get().getType()).isEqualTo(NotificationSettingType.POST_COMMENT);
        }

        @Test
        @DisplayName("존재하지 않는 타입 조회 시 빈 Optional 반환")
        void t3() {
            // when
            Optional<NotificationSetting> settingOpt = notificationSettingRepository.findByUserIdAndType(
                    userWithSettings.getId(),
                    NotificationSettingType.SYSTEM // 저장하지 않은 타입
            );

            // then
            assertThat(settingOpt).isEmpty();
        }

        @Test
        @DisplayName("특정 사용자의 활성화된 알림 설정만 조회 성공")
        void t4() {
            // when
            List<NotificationSetting> enabledSettings = notificationSettingRepository.findEnabledSettingsByUserId(userWithSettings.getId());

            // then: setUp에서 활성화한 POST_COMMENT만 조회되어야 함
            assertThat(enabledSettings).hasSize(1);
            assertThat(enabledSettings.get(0).getType()).isEqualTo(NotificationSettingType.POST_COMMENT);
            assertThat(enabledSettings.get(0).isEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("알림 설정 변경 및 영속성 테스트")
    class ChangePersistenceTest {

        @Test
        @DisplayName("toggle 메서드 호출 후 변경사항 DB에 반영")
        void t1() {
            // given: POST_COMMENT 설정(enabled=true)을 조회
            NotificationSetting setting = notificationSettingRepository.findByUserIdAndType(
                    userWithSettings.getId(),
                    NotificationSettingType.POST_COMMENT
            ).orElseThrow();
            assertThat(setting.isEnabled()).isTrue();

            // when: toggle 메서드 호출
            setting.toggle();
            entityManager.flush(); // 변경 감지(Dirty Checking)를 통해 UPDATE 쿼리 실행
            entityManager.clear(); // 영속성 컨텍스트 초기화

            // then: 다시 조회했을 때 enabled 상태가 false로 변경되어 있어야 함
            NotificationSetting updatedSetting = notificationSettingRepository.findById(setting.getId()).orElseThrow();
            assertThat(updatedSetting.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("데이터 제약 조건 테스트")
    class ConstraintTest {

        @Test
        @DisplayName("동일한 사용자와 타입으로 중복 설정 저장 시 예외 발생")
        void t1() {
            // given: 이미 POST_COMMENT 타입 설정이 존재하는 사용자
            // when: 동일한 타입으로 새로운 설정을 생성
            NotificationSetting duplicateSetting = NotificationSetting.create(
                    userWithSettings,
                    NotificationSettingType.POST_COMMENT
            );

            // then: Unique 제약 조건 위반으로 DataIntegrityViolationException이 발생해야 함
            assertThrows(DataIntegrityViolationException.class, () -> {
                notificationSettingRepository.saveAndFlush(duplicateSetting);
            });
        }
    }
}