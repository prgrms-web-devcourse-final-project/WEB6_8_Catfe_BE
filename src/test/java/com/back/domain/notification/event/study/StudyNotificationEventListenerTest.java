package com.back.domain.notification.event.study;

import com.back.domain.notification.entity.NotificationSettingType;
import com.back.domain.notification.service.NotificationService;
import com.back.domain.user.entity.User;
import com.back.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StudyNotificationEventListener 테스트")
class StudyNotificationEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private StudyNotificationEventListener listener;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@test.com")
                .build();
    }

    // ====================== 학습 기록 생성 이벤트 ======================

    @Test
    @DisplayName("학습 기록 생성 이벤트 수신 - 알림 생성 성공")
    void t1() {
        // given
        StudyRecordCreatedEvent event = new StudyRecordCreatedEvent(
                1L,   // userId
                100L, // studyRecordId
                50L,  // studyPlanId
                3600L  // duration (1시간)
        );

        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

        // when
        listener.handleStudyRecordCreated(event);

        // then
        verify(notificationService).createSelfNotification(
                eq(testUser), // 본인
                anyString(),  // title
                anyString(),  // content
                eq("/study/records/100"),
                eq(NotificationSettingType.SYSTEM)
        );
    }

    @Test
    @DisplayName("학습 기록 생성 이벤트 - 사용자 없음")
    void t2() {
        // given
        StudyRecordCreatedEvent event = new StudyRecordCreatedEvent(
                999L, // 존재하지 않는 userId
                100L,
                50L,
                3600L
        );

        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // when
        listener.handleStudyRecordCreated(event);

        // then
        verify(notificationService, never()).createSelfNotification(
                any(), anyString(), anyString(), anyString(), any(NotificationSettingType.class)
        );
    }

    @Test
    @DisplayName("학습 기록 생성 이벤트 - 짧은 학습 시간")
    void t3() {
        // given
        StudyRecordCreatedEvent event = new StudyRecordCreatedEvent(
                1L,
                100L,
                50L,
                300L  // 5분
        );

        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

        // when
        listener.handleStudyRecordCreated(event);

        // then
        verify(notificationService).createSelfNotification(
                eq(testUser),
                anyString(),
                anyString(),
                eq("/study/records/100"),
                eq(NotificationSettingType.SYSTEM)
        );
    }

    @Test
    @DisplayName("학습 기록 생성 이벤트 - 긴 학습 시간")
    void t4() {
        // given
        StudyRecordCreatedEvent event = new StudyRecordCreatedEvent(
                1L,
                100L,
                50L,
                14400L  // 4시간
        );

        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

        // when
        listener.handleStudyRecordCreated(event);

        // then
        verify(notificationService).createSelfNotification(
                eq(testUser),
                anyString(),
                anyString(),
                eq("/study/records/100"),
                eq(NotificationSettingType.SYSTEM)
        );
    }

    // ====================== 일일 목표 달성 이벤트 ======================

    @Test
    @DisplayName("일일 목표 달성 이벤트 수신 - 알림 생성 성공")
    void t5() {
        // given
        LocalDate today = LocalDate.now();
        DailyGoalAchievedEvent event = new DailyGoalAchievedEvent(
                1L,    // userId
                today, // achievedDate
                5,     // completedPlans
                5      // totalPlans
        );

        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

        // when
        listener.handleDailyGoalAchieved(event);

        // then
        verify(notificationService).createSelfNotification(
                eq(testUser), // 본인
                anyString(),  // title
                anyString(),  // content
                eq("/study/plans?date=" + today),
                eq(NotificationSettingType.SYSTEM)
        );
    }

    @Test
    @DisplayName("일일 목표 달성 이벤트 - 사용자 없음")
    void t6() {
        // given
        LocalDate today = LocalDate.now();
        DailyGoalAchievedEvent event = new DailyGoalAchievedEvent(
                999L, // 존재하지 않는 userId
                today,
                5,
                5
        );

        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // when
        listener.handleDailyGoalAchieved(event);

        // then
        verify(notificationService, never()).createSelfNotification(
                any(), anyString(), anyString(), anyString(), any(NotificationSettingType.class)
        );
    }

    @Test
    @DisplayName("일일 목표 달성 이벤트 - 부분 달성")
    void t7() {
        // given
        LocalDate today = LocalDate.now();
        DailyGoalAchievedEvent event = new DailyGoalAchievedEvent(
                1L,
                today,
                3,  // completedPlans
                5   // totalPlans
        );

        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

        // when
        listener.handleDailyGoalAchieved(event);

        // then
        verify(notificationService).createSelfNotification(
                eq(testUser),
                anyString(),
                anyString(),
                eq("/study/plans?date=" + today),
                eq(NotificationSettingType.SYSTEM)
        );
    }

    @Test
    @DisplayName("일일 목표 달성 이벤트 - 초과 달성")
    void t8() {
        // given
        LocalDate today = LocalDate.now();
        DailyGoalAchievedEvent event = new DailyGoalAchievedEvent(
                1L,
                today,
                7,  // completedPlans
                5   // totalPlans (목표보다 많이 달성)
        );

        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

        // when
        listener.handleDailyGoalAchieved(event);

        // then
        verify(notificationService).createSelfNotification(
                eq(testUser),
                anyString(),
                anyString(),
                eq("/study/plans?date=" + today),
                eq(NotificationSettingType.SYSTEM)
        );
    }

    @Test
    @DisplayName("일일 목표 달성 이벤트 - 과거 날짜")
    void t9() {
        // given
        LocalDate yesterday = LocalDate.now().minusDays(1);
        DailyGoalAchievedEvent event = new DailyGoalAchievedEvent(
                1L,
                yesterday,
                5,
                5
        );

        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

        // when
        listener.handleDailyGoalAchieved(event);

        // then
        verify(notificationService).createSelfNotification(
                eq(testUser),
                anyString(),
                anyString(),
                eq("/study/plans?date=" + yesterday),
                eq(NotificationSettingType.SYSTEM)
        );
    }

    // ====================== 예외 처리 테스트 ======================

    @Test
    @DisplayName("학습 기록 알림 생성 중 예외 발생 - 로그만 출력하고 예외 전파 안함")
    void t10() {
        // given
        StudyRecordCreatedEvent event = new StudyRecordCreatedEvent(
                1L, 100L, 3600L, 50L
        );

        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

        willThrow(new RuntimeException("알림 생성 실패"))
                .given(notificationService).createSelfNotification(
                        any(), anyString(), anyString(), anyString(), any(NotificationSettingType.class)
                );

        // when & then - 예외가 전파되지 않아야 함
        assertThatCode(() -> listener.handleStudyRecordCreated(event))
                .doesNotThrowAnyException();

        verify(notificationService).createSelfNotification(
                any(), anyString(), anyString(), anyString(), any(NotificationSettingType.class)
        );
    }

    @Test
    @DisplayName("일일 목표 달성 알림 생성 중 예외 발생 - 로그만 출력하고 예외 전파 안함")
    void t11() {
        // given
        LocalDate today = LocalDate.now();
        DailyGoalAchievedEvent event = new DailyGoalAchievedEvent(
                1L, today, 5, 5
        );

        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

        willThrow(new RuntimeException("알림 생성 실패"))
                .given(notificationService).createSelfNotification(
                        any(), anyString(), anyString(), anyString(), any(NotificationSettingType.class)
                );

        // when & then - 예외가 전파되지 않아야 함
        assertThatCode(() -> listener.handleDailyGoalAchieved(event))
                .doesNotThrowAnyException();

        verify(notificationService).createSelfNotification(
                any(), anyString(), anyString(), anyString(), any(NotificationSettingType.class)
        );
    }
}